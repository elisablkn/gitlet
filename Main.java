package gitlet;

import java.text.ParseException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Elizaveta Belkina
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws ParseException {

        if (args.length < 1) {
            System.out.println("Please enter a command.");
        } else {
            int numArgs = args.length;
            switchCase1(numArgs, args);
        }
        System.exit(0);
    }

    static void helperCheckout(int numArgs, String[] args) {
        if ((numArgs == 3) && args[1].equals("--")) {
            Command.checkoutFile(args[2]);
        } else if ((numArgs == 4) && args[2].equals("--")) {
            Command.checkoutCommit(args[1], args[3]);
        } else if (checkArgs(numArgs, 2)) {
            Command.checkoutBranch(args[1]);
        }
    }

    static void switchCase1(int numArgs, String[] args) throws ParseException {
        switch (args[0]) {
        case "init":
            if (checkArgs(numArgs, 1)) {
                Command.init();
            }
            break;
        case "add":
            if (checkArgs(numArgs, 2)) {
                Command.add(args[1]);
            }
            break;
        case "commit":
            if (checkArgs(numArgs, 2)) {
                Command.commit(args[1]);
            }
            break;
        case "rm":
            if (checkArgs(numArgs, 2)) {
                Command.rm(args[1]);
            }
            break;
        case "log":
            if (checkArgs(numArgs, 1)) {
                Command.log();
            }
            break;
        case "global-log":
            if (checkArgs(numArgs, 1)) {
                Command.globalLog();
            }
            break;
        case "find":
            if (checkArgs(numArgs, 2)) {
                Command.find(args[1]);
            }
            break;
        case "status":
            if (checkArgs(numArgs, 1)) {
                Command.status();
            }
            break;
        case "checkout":
            helperCheckout(numArgs, args);
            break;
        case "branch":
            if (checkArgs(numArgs, 2)) {
                Command.branch(args[1]);
            }
            break;
        case "rm-branch":
            if (checkArgs(numArgs, 2)) {
                Command.removeBranch(args[1]);
            }
            break;
        default:
            switchCase2(numArgs, args);
            break;
        }
    }

    static void switchCase2(int numArgs, String[] args) throws ParseException {
        switch (args[0]) {
        case "reset":
            if (checkArgs(numArgs, 2)) {
                Command.reset(args[1]);
            }
            break;
        case "merge":
            if (checkArgs(numArgs, 2)) {
                Command.merge(args[1]);
            }
            break;
        default:
            System.out.println("No command with that name exists.");
            break;
        }
    }

    static boolean checkArgs(int arguments, int expectedNum) {
        if (arguments == expectedNum) {
            return true;
        } else {
            System.out.println("Incorrect operands.");
            return false;
        }
    }
}
