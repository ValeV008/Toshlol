"""Regression checks for the host runner's pass/fail decisions; no emulator required."""

import unittest
import xml.etree.ElementTree as ET

from verify_overview_persistence import (
    assert_activity,
    assert_drawer,
    assert_instrumentation_passed,
    assert_overview,
)


def overview(balance="EUR 1002.00", income="2.00", expense="0.00", net="2.00"):
    root = ET.Element("hierarchy")
    for label, amount in (("CLOSING BALANCE", balance), ("Income", income), ("Expenses", expense), ("Net", net)):
        group = ET.SubElement(root, "node")
        ET.SubElement(group, "node", text=label)
        ET.SubElement(group, "node", text=amount)
    return root


class OverviewPersistenceAssertionsTest(unittest.TestCase):
    def test_accepts_selected_and_empty_totals(self):
        assert_overview(overview(), True)
        assert_overview(overview("EUR 0.00", "0.00", net="0.00"), False)

    def test_rejects_all_accounts_fallback_and_wrong_period_flows(self):
        for root in (overview("EUR 6009.00", "9.00", net="9.00"), overview(income="9.00"),
                     overview(expense="3.00"), overview(net="9.00")):
            with self.subTest(xml=ET.tostring(root)), self.assertRaises(AssertionError):
                assert_overview(root, True)

    def test_rejects_missing_summary_even_if_account_card_has_expected_balance(self):
        root = overview("EUR 0.00")
        ET.SubElement(root, "node", text="EUR 1002.00")
        with self.assertRaises(AssertionError):
            assert_overview(root, True)

    def test_associates_each_amount_with_its_label_in_flat_android_hierarchy(self):
        grouped = overview()
        root = ET.Element("hierarchy")
        for group in grouped:
            root.extend(group)
        assert_overview(root, True)
        root[5].set("text", "2.00")  # Expenses; 0.00 exists elsewhere in the same card.
        with self.assertRaises(AssertionError):
            assert_overview(root, True)

    def test_rejects_selected_totals_for_empty_selection(self):
        with self.assertRaises(AssertionError):
            assert_overview(overview(), False)

    def test_rejects_selection_prompt(self):
        root = overview("EUR 0.00", "0.00", net="0.00")
        ET.SubElement(root, "node", text="Select Accounts")
        with self.assertRaises(AssertionError):
            assert_overview(root, False)

    def test_requires_matching_activity_and_excludes_unselected_account(self):
        root = ET.fromstring('<hierarchy><node text="Income · EUR 2.00" /></hierarchy>')
        assert_activity(root, True)
        with self.assertRaises(AssertionError):
            assert_activity(root, False)
        ET.SubElement(root, "node", text="Income · EUR 7.00")
        with self.assertRaises(AssertionError):
            assert_activity(root, True)

    def test_requires_empty_activity_message(self):
        root = ET.fromstring('<hierarchy><node text="No activity for this month and Account selection." /></hierarchy>')
        assert_activity(root, False)
        with self.assertRaises(AssertionError):
            assert_activity(ET.fromstring("<hierarchy />"), False)

    def test_checks_both_drawer_switches(self):
        root = ET.fromstring('<hierarchy><node content-desc="Cash Account selection" checked="true" />'
                             '<node content-desc="Savings Account selection" checked="false" /></hierarchy>')
        assert_drawer(root, True)
        with self.assertRaises(AssertionError):
            assert_drawer(root, False)
        root[1].set("checked", "true")
        with self.assertRaises(AssertionError):
            assert_drawer(root, True)
        root[0].set("checked", "false")
        root[1].set("checked", "false")
        assert_drawer(root, False)

    def test_requires_exactly_one_successful_seed_test(self):
        assert_instrumentation_passed("INSTRUMENTATION_CODE: -1\n\nOK (1 test)\n")
        for output in ("", "OK (0 tests)", "OK (2 tests)", "FAILURES!!!\nTests run: 1, Failures: 1",
                       "INSTRUMENTATION_FAILED", "OK (1 test)\nINSTRUMENTATION_RESULT: shortMsg=Process crashed"):
            with self.subTest(output=output), self.assertRaises(AssertionError):
                assert_instrumentation_passed(output)


if __name__ == "__main__":
    unittest.main()
