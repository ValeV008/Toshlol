"""Verify Overview selection after real process death on an API 29 test emulator.

The seed tests replace the emulator app's ledger. Requires Python 3.10+ and adb.
"""

import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone


ROOT = Path(__file__).resolve().parents[1]
TEST_CLASS = "com.toshlol.personalfinance.OverviewPersistenceHandoffTest"
ACTIVITY = "com.toshlol.personalfinance.MainActivity"
EMPTY_ACTIVITY = "No activity for this month and Account selection."


def get_texts(root):
    return [node.get("text", "") for node in root.iter("node")]


def assert_overview(root, selected):
    expected = {
        "CLOSING BALANCE": "EUR 1002.00" if selected else "EUR 0.00",
        "Income": "2.00" if selected else "0.00",
        "Expenses": "0.00",
        "Net": "2.00" if selected else "0.00",
    }
    parents = {child: parent for parent in root.iter() for child in parent}
    summary = find_overview_summary(root, parents, expected.keys())
    for label, amount in expected.items():
        labels = [node for node in summary.iter("node") if node.get("text") == label]
        if len(labels) != 1:
            raise AssertionError(f"Expected one {label} label; observed {get_texts(root)}")
        siblings = list(parents[labels[0]])
        index = siblings.index(labels[0]) + 1
        if index >= len(siblings) or amount not in get_texts(siblings[index]):
            raise AssertionError(f"Expected {label}: {amount}; observed {get_texts(root)}")
    assert_no_selection_prompt(root)


def find_overview_summary(root, parents, labels):
    anchors = [node for node in root.iter("node") if node.get("text") == "CLOSING BALANCE"]
    if len(anchors) != 1:
        raise AssertionError(f"Expected one CLOSING BALANCE label; observed {get_texts(root)}")
    summary = parents[anchors[0]]
    while not set(labels).issubset(get_texts(summary)):
        if summary not in parents:
            raise AssertionError(f"Missing Overview summary labels; observed {get_texts(root)}")
        summary = parents[summary]
    return summary


def assert_no_selection_prompt(root):
    if any("select accounts" in text.casefold() for text in get_texts(root)):
        raise AssertionError("Empty selection must not show a Select Accounts prompt")


def assert_activity(root, selected):
    texts = get_texts(root)
    expected = "Income \u00b7 EUR 2.00" if selected else EMPTY_ACTIVITY
    if expected not in texts:
        raise AssertionError(f"Expected activity {expected!r}; observed {texts}")
    if "Income \u00b7 EUR 7.00" in texts or (not selected and "Income \u00b7 EUR 2.00" in texts):
        raise AssertionError("Activity includes an unselected Account")
    assert_no_selection_prompt(root)


def assert_drawer(root, selected):
    for account, checked in (("Cash", selected), ("Savings", False)):
        switches = [node for node in root.iter("node") if node.get("content-desc") == f"{account} Account selection"]
        if len(switches) != 1 or switches[0].get("checked") != str(checked).lower():
            raise AssertionError(f"Expected {account} switch checked={checked}")


def assert_instrumentation_passed(output):
    # am instrument can return exit code zero even when a test crashes or fails.
    if not re.search(r"^OK \(1 test\)\s*$", output, re.MULTILINE):
        raise AssertionError(f"Seed instrumentation did not pass exactly one test:\n{output}")
    if re.search(r"FAILURES|INSTRUMENTATION_FAILED|shortMsg=|Process crashed", output):
        raise AssertionError(f"Seed instrumentation failed:\n{output}")


class HostRunner:
    def __init__(self, adb, serial, output):
        self.adb = [adb, "-s", serial]
        self.output = output
        self.output.mkdir(parents=True, exist_ok=False)

    def run(self, command, timeout=60, allow_failure=False):
        result = subprocess.run(command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
        output = result.stdout.decode("utf-8", errors="replace")
        with (self.output / "commands.log").open("a", encoding="utf-8") as log:
            log.write(f"$ {command!r}\n{output}\n[exit {result.returncode}]\n")
        if result.returncode and not allow_failure:
            raise RuntimeError(f"Command failed ({result.returncode}): {command!r}\n{output[-6000:]}")
        return output

    def shell(self, *arguments, **options):
        return self.run([*self.adb, "shell", *arguments], **options)

    def read_hierarchy(self, name):
        remote = "/sdcard/overview-persistence.xml"
        self.shell("rm", "-f", remote)
        self.shell("uiautomator", "dump", remote, timeout=15)
        xml = self.run([*self.adb, "exec-out", "cat", remote], timeout=15)
        (self.output / f"{name}.xml").write_text(xml, encoding="utf-8")
        return ET.fromstring(xml)

    def wait_for_ui(self, name, assertion, selected, retry_action=None):
        deadline = time.monotonic() + 45
        last_error = None
        while time.monotonic() < deadline:
            try:
                root = self.read_hierarchy(name)
                assertion(root, selected)
                self.capture_screenshot(name)
                return
            except (AssertionError, ET.ParseError, RuntimeError, subprocess.TimeoutExpired) as error:
                last_error = error
                if retry_action is not None:
                    retry_action()
                time.sleep(0.5)
        raise AssertionError(f"Timed out waiting for {name}: {last_error}")

    def capture_screenshot(self, name):
        remote = "/sdcard/overview-persistence.png"
        self.shell("screencap", "-p", remote)
        self.run([*self.adb, "pull", remote, str(self.output / f"{name}.png")])

    def verify_state(self, selected, package, test_package):
        state = "selected" if selected else "empty"
        method = f"prepare{state.capitalize()}OverviewForHostProcessRestart"
        print(f"Verifying {state} selection after host-driven process restart...", flush=True)
        output = self.shell("am", "instrument", "-w", "-e", "class", f"{TEST_CLASS}#{method}",
                            f"{test_package}/androidx.test.runner.AndroidJUnitRunner", timeout=120)
        (self.output / f"{state}-seed.txt").write_text(output, encoding="utf-8")
        assert_instrumentation_passed(output)
        self.shell("am", "force-stop", package)
        if self.shell("pidof", package, allow_failure=True).strip():
            raise AssertionError("App process still running after force-stop")
        launch = self.shell("am", "start", "-W", "-n", f"{package}/{ACTIVITY}")
        # Android's launch wait can expire before the first frame on a slow emulator.
        # A timeout still needs to pass every bounded post-restart UI assertion below.
        if not re.search(r"^Status: (?:ok|timeout)\s*$", launch, re.MULTILINE):
            raise AssertionError(f"App launch failed: {launch}")
        self.wait_for_ui(f"{state}-overview", assert_overview, selected)
        size = self.shell("wm", "size")
        width, height = map(int, re.findall(r"(\d+)x(\d+)", size)[-1])
        # Activity is below the fold on small devices. Scroll actual UI, without changing selection.
        self.shell("input", "swipe", str(width // 2), str(height * 3 // 4),
                   str(width // 2), str(height // 3), "400")
        for attempt in range(5):
            root = self.read_hierarchy(f"{state}-activity")
            try:
                assert_activity(root, selected)
                break
            except AssertionError:
                if attempt == 4:
                    raise
                self.shell("input", "swipe", str(width // 2), str(height * 3 // 4),
                           str(width // 2), str(height // 3), "400")
        self.capture_screenshot(f"{state}-activity")
        def open_drawer():
            self.shell("input", "swipe", str(width - 2), str(height // 2),
                       str(width * 70 // 100), str(height // 2), "700")
        open_drawer()
        self.wait_for_ui(f"{state}-drawer", assert_drawer, selected, retry_action=open_drawer)


def find_device(adb, serial):
    result = subprocess.run([adb, "devices"], check=True, capture_output=True, text=True, timeout=15)
    devices = [line.split()[0] for line in result.stdout.splitlines()[1:] if line.endswith("\tdevice")]
    if serial:
        if serial not in devices:
            raise RuntimeError(f"Device {serial} is not online; available devices: {devices}")
        return serial
    if len(devices) != 1:
        raise RuntimeError(f"Specify --serial for one test emulator; available devices: {devices}")
    return devices[0]


def load_apk(directory):
    metadata = json.loads((directory / "output-metadata.json").read_text(encoding="utf-8"))
    elements = metadata["elements"]
    if len(elements) != 1 or not (directory / elements[0]["outputFile"]).is_file():
        raise RuntimeError(f"Expected one built APK in {directory}")
    return metadata["applicationId"], directory / elements[0]["outputFile"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
    parser.add_argument("--adb", default=shutil.which("adb"))
    parser.add_argument("--skip-build", action="store_true", help="Install existing debug APKs instead of rebuilding")
    parser.add_argument("--output-dir", type=Path, help="New evidence directory (must not already exist)")
    arguments = parser.parse_args()
    if not arguments.adb:
        parser.error("adb must be on PATH or supplied with --adb")
    serial = find_device(arguments.adb, arguments.serial)
    output = arguments.output_dir or ROOT / "build" / "overview-persistence" / datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    runner = HostRunner(arguments.adb, serial, output.resolve())
    print(f"Evidence: {runner.output}", flush=True)
    try:
        if runner.shell("getprop", "ro.kernel.qemu").strip() != "1":
            raise RuntimeError("Use a test emulator: seed tests replace the app ledger")
        if runner.shell("getprop", "ro.build.version.sdk").strip() != "29":
            raise RuntimeError("This verification requires an Android 10 (API 29) emulator")
        if not arguments.skip_build:
            wrapper = "gradlew.bat" if os.name == "nt" else "gradlew"
            runner.run([str(ROOT / wrapper), ":app:assembleDebug", ":app:assembleDebugAndroidTest", "--console=plain"], timeout=600)
        package, app_apk = load_apk(ROOT / "app/build/outputs/apk/debug")
        test_package, test_apk = load_apk(ROOT / "app/build/outputs/apk/androidTest/debug")
        for apk in (app_apk, test_apk):
            runner.run([*runner.adb, "install", "-r", str(apk)], timeout=120)
        runner.shell("input", "keyevent", "KEYCODE_WAKEUP")
        runner.shell("wm", "dismiss-keyguard")
        for selected in (True, False):
            runner.verify_state(selected, package, test_package)
        (runner.output / "result.json").write_text(json.dumps({"serial": serial, "package": package,
                                                               "selected": "passed", "empty": "passed"}, indent=2), encoding="utf-8")
        print(f"Overview persistence passed. Evidence: {runner.output}")
    except Exception:
        try:
            runner.read_hierarchy("failure")
            runner.capture_screenshot("failure")
        except Exception as error:
            (runner.output / "capture-error.txt").write_text(str(error), encoding="utf-8")
        raise
    finally:
        try:
            logcat = runner.run([*runner.adb, "logcat", "-d", "-t", "500"])
            (runner.output / "logcat.txt").write_text(logcat, encoding="utf-8")
        except Exception as error:
            (runner.output / "logcat-error.txt").write_text(str(error), encoding="utf-8")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, OSError, RuntimeError, subprocess.SubprocessError, ValueError) as error:
        print(f"Overview persistence failed: {error}", file=sys.stderr)
        sys.exit(1)
