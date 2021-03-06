#!/usr/bin/python3

"""
Test kit for executable, SPIM, and Logisim files.
Uses provided settings file to test assignment.
"""

import argparse
from common import Grader

def main():
    """
    Parse arguments and run autograder.
    """
    parser = argparse.ArgumentParser(description="Run autograder.")
    parser.add_argument("test_suite", type=str, default="ALL",
                        help="ALL, CLEAN, or test suite name")
    parser.add_argument("--settings", type=str, default="settings.json",
                        help="settings file to use for grading (default=settings.json)")
    parser.add_argument('--make-expected', help=argparse.SUPPRESS, action='store_true') # not for student use! assumes program is correct and uses it to generate the expected outputs
    parser.add_argument('--mode', type=str, default="") # Override default mode specified by settings.json
    parser.add_argument('--extra-credit-multiplier', type=float, default=1.0) # Multiply final score by this

    args = parser.parse_args()

    grader = Grader(args.test_suite, args.settings, args.make_expected, args.mode, args.extra_credit_multiplier)
    _ = grader.run()

if __name__ == "__main__":
    main()
