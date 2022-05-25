package gitlet;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayDeque;


public class Command {

    /** Current Working Directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** Gitlet. */
    static final File GIT = new File(CWD + "/" + ".gitlet");

    /** Staging Area file. */
    static final File INDEX = new File(CWD + "/" + ".gitlet/index.txt");

    /** Logs. */
    static final File LOGS = new File(CWD + "/" + ".gitlet/logs");

    /** Head commit. */
    static final File HEAD = new File(CWD + "/" + ".gitlet/head.txt");

    /** Master branch. */
    static final File MASTER = new File(CWD + "/" + ".gitlet/master.txt");

    /** Blobs. */
    static final File BLOBS = new File(CWD + "/" + ".gitlet/blobs");

    /** Commits. */
    static final File COMMITS = new File(CWD + "/" + ".gitlet/commits");

    /** Current branch. */
    static final File CURRENT_BRANCH = new File(CWD
            + "/" + ".gitlet/currbranch.txt");

    /** Staging Area. */
    private static Index stagingArea = new Index();

    public static Commit getCommit() {
        String commitName = Utils.readContentsAsString(HEAD);
        File commitFile = new File(CWD + "/" + ".gitlet/commits/" + commitName);
        Commit currentCommit = Utils.readObject(commitFile, Commit.class);
        return currentCommit;
    }

    public static void init() {
        if (GIT.exists()) {
            System.out.println("A Gitlet version-control system already "
                   + "exists in the current directory.");
        } else {
            GIT.mkdir();
            BLOBS.mkdir();
            COMMITS.mkdir();

            LOGS.mkdir();
            stagingArea = new Index();
            ZoneId zone = ZoneId.systemDefault();
            Commit initial = new Commit("initial commit", null,
                    "1970-01-01 00:00:00", null);


            String commitName = Utils.sha1(Utils.serialize(initial));
            File initialCommit = new File(
                    CWD + "/" + ".gitlet/commits/" + commitName);

            try {
                HEAD.createNewFile();
                MASTER.createNewFile();
                INDEX.createNewFile();
                initialCommit.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String currentBranch = "master";
            Utils.writeContents(CURRENT_BRANCH, currentBranch);

            Utils.writeObject(INDEX, stagingArea);
            Utils.writeContents(initialCommit, Utils.serialize(initial));
            Utils.writeContents(MASTER, commitName);
            Utils.writeContents(HEAD, commitName);
        }

    }

    public static void add(String filename) {
        stagingArea = Utils.readObject(INDEX, Index.class);
        File newFile = new File(CWD + "/" + filename);
        boolean fileInDir = new File(CWD, filename).exists();
        if (!fileInDir) {
            System.out.println("File does not exist.");
        } else {
            String blob = Utils.sha1(Utils.readContentsAsString(newFile));
            Commit currentCommit = getCommit();
            if (currentCommit.getBlob(filename).equals(blob)
                    && stagingArea.fileExists(filename)) {
                stagingArea.remove((filename));
            } else if (stagingArea.fileExistsToDelete(filename)) {
                stagingArea.remove(filename);
            } else if (currentCommit.getBlob(filename).equals(blob)) {
                return;
            } else {
                stagingArea.toStage(filename, blob);
                File newBlob = new File(
                        CWD + "/.gitlet/blobs/" + blob + ".txt");
                try {
                    newBlob.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Utils.writeContents(newBlob, Utils.readContents(newFile));
            }
        }

        Utils.writeObject(INDEX, stagingArea);
    }

    public static void commit(String message) {
        DateTimeFormatter dateTime = DateTimeFormatter.
                ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        Commit newCommit = new Commit(message,
                getCommit(), dateTime.format(now), null);

        stagingArea = Utils.readObject(INDEX, Index.class);
        if (stagingArea.empty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        @SuppressWarnings({"unchecked"})
        TreeMap<String, String> trackedFiles = (TreeMap<String, String>)
                getCommit().getContents().clone();
        TreeMap<String, String> commitFiles = getCommit().getContents();
        @SuppressWarnings({"unchecked"})
        TreeMap<String, String> add = (TreeMap<String, String>)
                stagingArea.getAdd().clone();
        TreeMap<String, String> remove = stagingArea.getRemove();

        for (Map.Entry<String, String> pair : commitFiles.entrySet()) {
            String filename = pair.getKey();
            if (add.containsKey(filename)) {
                trackedFiles.remove(filename);
                trackedFiles.put(pair.getKey(), add.get(filename));
                add.remove(filename);
            } else if (remove.containsKey(filename)) {
                trackedFiles.remove(filename);
            }
        }
        if (!add.isEmpty()) {
            for (Map.Entry<String, String> pair : add.entrySet()) {
                trackedFiles.put(pair.getKey(), pair.getValue());
            }
        }
        newCommit.setContents(trackedFiles);

        String commitName = Utils.sha1(Utils.serialize(newCommit));
        File commitFile = new File(CWD + "/.gitlet/commits/" + commitName);
        String currentBranch = Utils.readContentsAsString(CURRENT_BRANCH);
        File branchPointer = new File(CWD + "/.gitlet/"
                + currentBranch + ".txt");
        try {
            commitFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.writeObject(commitFile, newCommit);
        stagingArea.reset();
        Utils.writeObject(INDEX, stagingArea);
        Utils.writeContents(HEAD, commitName);
        Utils.writeContents(branchPointer, commitName);
    }

    public static void rm(String filename) {
        stagingArea = Utils.readObject(INDEX, Index.class);

        if (!stagingArea.fileExists(filename)
                && !getCommit().getContents().containsKey(filename)) {
            System.out.println("No reason to remove the file.");
            return;
        }

        if (stagingArea.fileExistsToAdd(filename)) {
            stagingArea.remove(filename);
        }

        if (getCommit().getContents().containsKey(filename)) {
            String blob = getCommit().getBlob(filename);
            stagingArea.toRemove(filename, blob);
            File removeFile = new File(CWD + "/" + filename);
            Utils.restrictedDelete(removeFile);
        }

        Utils.writeObject(INDEX, stagingArea);
    }

    public static void log() throws ParseException {
        File dir = new File(CWD + "/" + ".gitlet");
        if (!dir.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        Commit currentCommit = getCommit();
        while (currentCommit != null) {
            String commitName = Utils.sha1(Utils.serialize(currentCommit));
            currentCommit.printCommit(commitName);
            currentCommit = currentCommit.getParent();
        }
    }

    public static void globalLog() throws ParseException {
        File dir = new File(CWD + "/" + ".gitlet/commits");
        List<String> filenames = Utils.plainFilenamesIn(dir);
        for (String filename : filenames) {
            File commitFile = new File(CWD + "/"
                    + ".gitlet/commits/" + filename);
            Commit commit = Utils.readObject(commitFile, Commit.class);
            commit.printCommit(filename);
        }
    }

    public static void find(String commitMessage) {
        File dir = new File(CWD + "/" + ".gitlet/commits");
        List<String> filenames = Utils.plainFilenamesIn(dir);
        boolean commitExists = false;
        for (String filename : filenames) {
            File commitFile = new File(CWD + "/"
                    + ".gitlet/commits/" + filename);
            Commit commit = Utils.readObject(commitFile, Commit.class);
            if (commit.getMessage().equals(commitMessage)) {
                System.out.println(filename);
                commitExists = true;
            }
        }

        if (!commitExists) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        File dir = new File(CWD + "/" + ".gitlet");
        if (!dir.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        String currentBranch = Utils.readContentsAsString(CURRENT_BRANCH);
        String branchName = currentBranch + ".txt";
        List<String> filenames = Utils.plainFilenamesIn(dir);

        System.out.println("=== Branches ===");

        for (String filename : filenames) {
            if (branchName.equals(filename)) {
                System.out.println("*" + currentBranch);
            } else if (!filename.equals("currbranch.txt")
                    && !filename.equals("head.txt")
                    && !filename.equals("index.txt")) {
                File file = new File(GIT + "/" + filename);
                String contents = Utils.readContentsAsString(file);
                if (!contents.equals("")) {
                    System.out.println(filename.
                            substring(0, filename.length() - 4));
                }
            }
        }

        System.out.println();
        System.out.println("=== Staged Files ===");

        stagingArea = Utils.readObject(INDEX, Index.class);

        TreeMap<String, String> filesToAddition = stagingArea.getAdd();
        for (Map.Entry<String, String> pair : filesToAddition.entrySet()) {
            System.out.println(pair.getKey());
        }

        System.out.println();
        System.out.println("=== Removed Files ===");

        TreeMap<String, String> filesToRemove = stagingArea.getRemove();
        for (Map.Entry<String, String> pair : filesToRemove.entrySet()) {
            System.out.println(pair.getKey());
        }

        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    public static void checkoutFile(String filename) {
        Commit currentCommit = getCommit();
        checkout(Utils.readContentsAsString(HEAD), filename);
    }

    public static void checkout(String commit, String filename) {
        File commitFile = new File(CWD + "/" + ".gitlet/commits/" + commit);
        Commit currentCommit = Utils.readObject(commitFile, Commit.class);
        if (!currentCommit.getContents().containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            File file = new File(CWD + "/" + filename);
            if (file.exists()) {
                Utils.restrictedDelete(file);
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String blob = currentCommit.getContents().get(filename);
            Utils.writeContents(file, Utils.readContentsAsString
                    (new File(CWD + "/.gitlet/blobs/" + blob + ".txt")));
        }
    }

    public static void checkoutCommit(String commit, String filename) {
        commit = abbreviatedUID(commit);
        File commitFile = new File(CWD + "/.gitlet/commits/" + commit);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        checkout(commit, filename);
    }

    public static void checkoutBranch(String branchName) {
        String currentBranch = Utils.readContentsAsString(CURRENT_BRANCH);
        if (currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        File branchFile = new File(CWD + "/.gitlet/" + branchName + ".txt");
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            return;
        }

        Commit currentCommit = getCommit();

        String commitName = Utils.readContentsAsString(branchFile);
        File fCommit = new File(CWD + "/.gitlet/commits/" + commitName);
        Commit finalCommit = Utils.readObject(fCommit, Commit.class);

        TreeMap<String, String> currentFiles = currentCommit.getContents();
        TreeMap<String, String> finalFiles = finalCommit.getContents();


        for (Map.Entry<String, String> toOverwrite : finalFiles.entrySet()) {
            File fileInCWD = new File(CWD + "/" + toOverwrite.getKey());
            if (!currentFiles.containsKey(toOverwrite.getKey())
                    && fileInCWD.exists()) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }

        Utils.writeContents(CURRENT_BRANCH, branchName);
        Utils.writeContents(HEAD, commitName);

        for (Map.Entry<String, String> toDelete : currentFiles.entrySet()) {
            if (!finalFiles.containsKey(toDelete.getKey())) {
                File delete = new File(CWD + "/" + toDelete.getKey());
                Utils.restrictedDelete(delete);
            }
        }

        for (Map.Entry<String, String> toOverwrite : finalFiles.entrySet()) {
            File newFile = new File(CWD + "/" + toOverwrite.getKey());
            if (!newFile.exists()) {
                try {
                    newFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File blob = new File(CWD + "/.gitlet/blobs/"
                    + toOverwrite.getValue() + ".txt");
            Utils.writeContents(newFile, Utils.readContents(blob));
        }
        stagingArea = Utils.readObject(INDEX, Index.class);
        stagingArea.reset();
        Utils.writeObject(INDEX, stagingArea);
    }

    public static void branch(String name) {
        File newBranch = new File(CWD + "/.gitlet/" + name + ".txt");

        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        try {
            newBranch.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.writeContents(newBranch, Utils.readContentsAsString(HEAD));

    }

    public static void removeBranch(String name) {
        File branch = new File(CWD + "/.gitlet/" + name + ".txt");
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        String currentBranch = Utils.readContentsAsString(CURRENT_BRANCH);
        if (currentBranch.equals(name)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branch.delete();
    }

    public static void reset(String commit) {
        File commitFile = new File(CWD + "/.gitlet/commits/" + commit);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit currentCommit = getCommit();

        Commit finalCommit = Utils.readObject(commitFile, Commit.class);

        TreeMap<String, String> currentFiles = currentCommit.getContents();
        TreeMap<String, String> finalFiles = finalCommit.getContents();

        for (Map.Entry<String, String> toOverwrite : finalFiles.entrySet()) {
            File fileInCWD = new File(CWD + "/" + toOverwrite.getKey());
            if (!currentFiles.containsKey(toOverwrite.getKey())
                    && fileInCWD.exists()) {
                System.out.println("There is an untracked file in the way; "
                       + "delete it, or add and commit it first.");
                return;
            }
        }

        for (Map.Entry<String, String> toDelete : currentFiles.entrySet()) {
            if (!finalFiles.containsKey(toDelete.getKey())) {
                File delete = new File(CWD + "/" + toDelete.getKey());
                Utils.restrictedDelete(delete);
            }
        }

        for (Map.Entry<String, String> toOverwrite : finalFiles.entrySet()) {
            File newFile = new File(CWD + "/" + toOverwrite.getKey());
            if (!newFile.exists()) {
                try {
                    newFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            File blob = new File(CWD + "/.gitlet/blobs/"
                    + toOverwrite.getValue() + ".txt");
            Utils.writeContents(newFile, Utils.readContents(blob));
        }

        String branch = Utils.readContentsAsString(CURRENT_BRANCH);
        File branchFile = new File(CWD + "/.gitlet/" + branch + ".txt");
        Utils.writeContents(branchFile, commit);
        Utils.writeContents(HEAD, commit);

        stagingArea = Utils.readObject(INDEX, Index.class);
        stagingArea.reset();
        Utils.writeObject(INDEX, stagingArea);

    }

    public static void mergeHelper1(TreeMap<String, String> filesHeadCommit,
                                    TreeMap<String, String> filesOtherCommit,
                                    TreeMap<String, String> filesSplitPoint,
                                    ArrayList<Boolean> conflict,
                                    TreeMap<String, String> trackedFiles,
                                    String commitID) {
        for (Map.Entry<String, String> files : filesSplitPoint.entrySet()) {
            String name = files.getKey();
            String blob = files.getValue();
            File file = new File(CWD + "/" + name);
            if (filesHeadCommit.containsKey(name)
                    && filesOtherCommit.containsKey(name)) {
                if (!filesOtherCommit.get(name).equals(blob)
                        && filesHeadCommit.get(name).equals(blob)) {
                    checkout(commitID, name);
                    stagingArea.toStage(name, filesOtherCommit.get(name));
                    trackedFiles.put(name, filesOtherCommit.get(name));
                } else if (filesOtherCommit.get(name).equals(blob)
                        && !filesHeadCommit.get(name).equals(blob)) {
                    checkoutFile(name);
                    trackedFiles.put(name, filesHeadCommit.get(name));
                } else if (!filesOtherCommit.get(name).equals(blob)
                        && !filesHeadCommit.get(name).equals(blob)) {
                    if (!filesOtherCommit.get(name).
                            equals(filesHeadCommit.get(name))) {
                        conflict.add(true);
                        overwriteConflict(name, filesHeadCommit,
                                filesOtherCommit);
                        String newBlob = Utils.sha1(Utils.
                                readContentsAsString(file));
                        trackedFiles.put(name, newBlob);
                    } else {
                        trackedFiles.put(name, filesHeadCommit.get(name));
                    }
                }
            } else if (filesHeadCommit.containsKey(name)
                    && filesHeadCommit.get(name).equals(blob)
                    && !filesOtherCommit.containsKey(name)) {
                stagingArea.remove(name);
                Utils.restrictedDelete(name);
            } else if (filesOtherCommit.containsKey(name)
                    && !filesHeadCommit.containsKey(name)
                    && filesOtherCommit.get(name).equals(blob)) {
                Utils.restrictedDelete(name);
            } else if (filesHeadCommit.containsKey(name)
                    && !filesHeadCommit.get(name).equals(blob)
                    && !filesOtherCommit.containsKey(name)) {
                conflict.add(true);
                TreeMap<String, String> removed = new TreeMap<>();
                removed.put(name, "");
                overwriteConflict(name, filesHeadCommit, removed);
                String newBlob = Utils.sha1(Utils.readContentsAsString(file));
                trackedFiles.put(name, newBlob);
            } else if (filesOtherCommit.containsKey(name)
                    && !filesOtherCommit.get(name).equals(blob)
                    && !filesHeadCommit.containsKey(name)) {
                conflict.add(true);
                TreeMap<String, String> removed = new TreeMap<>();
                removed.put(name, "");
                overwriteConflict(name, removed, filesOtherCommit);
                String newBlob = Utils.sha1(Utils.readContentsAsString(file));
                trackedFiles.put(name, newBlob);
            }
        }
    }

    public static void mergeHelper2(TreeMap<String, String> filesHeadCommit,
                                    TreeMap<String, String> filesOtherCommit,
                                    TreeMap<String, String> filesSplitPoint,
                                    ArrayList<Boolean> conflict,
                                    TreeMap<String, String> trackedFiles,
                                    String commitID) {
        for (Map.Entry<String, String> files : filesHeadCommit.entrySet()) {
            String name = files.getKey();
            if (!filesSplitPoint.containsKey(name)
                    && !filesOtherCommit.containsKey(name)) {
                checkoutFile(name);
                trackedFiles.put(name, files.getValue());
            }
        }
        for (Map.Entry<String, String> files : filesOtherCommit.entrySet()) {
            String name = files.getKey();
            if (!filesSplitPoint.containsKey(name)
                    && !filesHeadCommit.containsKey(name)) {
                checkoutCommit(commitID, name);
                trackedFiles.put(name, files.getValue());
            }
        }
    }

    public static void mergeHelper3(TreeMap<String, String> trackedFiles,
                                    String commitID, String branchName,
                                    String currentBranch, Commit otherCommit) {
        DateTimeFormatter dateTime = DateTimeFormatter.
                ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String message = "Merged " + branchName
                + " into " + currentBranch + ".";
        Commit newCommit = new Commit(message, getCommit(),
                dateTime.format(now), otherCommit);
        newCommit.setContents(trackedFiles);
        String commitName = Utils.sha1(Utils.serialize(newCommit));
        File newCommitFile = new File(CWD + "/.gitlet/commits/" + commitName);
        File branchPointer = new File(CWD
                + "/.gitlet/" + currentBranch + ".txt");
        try {
            newCommitFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(newCommitFile, newCommit);
        Utils.writeObject(INDEX, stagingArea);
        Utils.writeContents(HEAD, commitName);
        Utils.writeContents(branchPointer, commitName);
    }

    public static void merge(String branchName) {
        Commit headCommit = getCommit();
        File branch = new File(CWD + "/.gitlet/" + branchName + ".txt");
        String currentBranch = Utils.readContentsAsString(CURRENT_BRANCH);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branchName.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        String commitID = Utils.readContentsAsString(branch);
        File commitFile = new File(CWD + "/" + ".gitlet/commits/" + commitID);
        Commit otherCommit = Utils.readObject(commitFile, Commit.class);
        Commit splitPoint = findSplitPointAdvanced(headCommit, otherCommit);
        String splitPointName = Utils.sha1(Utils.serialize(splitPoint));
        String headCommitName = Utils.readContentsAsString(HEAD);
        if (splitPointName.equals(commitID)) {
            System.out.println("Given branch is an "
                   + "ancestor of the current branch.");
            return;
        }
        if (splitPointName.equals(headCommitName)) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        stagingArea = Utils.readObject(INDEX, Index.class);
        if (!stagingArea.empty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        TreeMap<String, String> filesHeadCommit = headCommit.getContents();
        TreeMap<String, String> filesOtherCommit = otherCommit.getContents();
        TreeMap<String, String> filesSplitPoint = splitPoint.getContents();
        List<String> filesInCWD = Utils.plainFilenamesIn(CWD);
        for (String file : filesInCWD) {
            File newFile = new File(CWD + "/" + file);
            String blob = Utils.sha1(Utils.readContentsAsString(newFile));
            if (!filesHeadCommit.containsKey(file)
                    && !filesOtherCommit.get(file).equals(blob)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        TreeMap<String, String> trackedFiles = new TreeMap<>();
        ArrayList<Boolean> conflict = new ArrayList<>();
        conflict.add(false);
        mergeHelper1(filesHeadCommit, filesOtherCommit,
                filesSplitPoint, conflict, trackedFiles, commitID);
        mergeHelper2(filesHeadCommit, filesOtherCommit,
                filesSplitPoint, conflict, trackedFiles, commitID);
        mergeHelper3(trackedFiles, commitID, branchName,
                currentBranch, otherCommit);
        if (conflict.get(conflict.size() - 1)) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    public static Commit findSplitPoint(Commit headCommit, Commit otherCommit) {
        ArrayList<Commit> headCommitTree = new ArrayList<>();
        ArrayList<Commit> otherCommitTree = new ArrayList<>();
        Commit currCommit = headCommit;
        while (currCommit != null) {
            headCommitTree.add(currCommit);
            currCommit = currCommit.getParent();
        }
        currCommit = otherCommit;
        while (currCommit != null) {
            otherCommitTree.add(currCommit);
            currCommit = currCommit.getParent();
        }

        for (Commit commit1 : headCommitTree) {
            for (Commit commit2 : otherCommitTree) {
                String commit1Name = Utils.sha1(Utils.serialize(commit1));
                String commit2Name = Utils.sha1(Utils.serialize(commit2));
                if (commit1Name.equals(commit2Name)) {
                    return commit1;
                }
            }
        }
        return getCommit();
    }

    public static Commit findSplitPointAdvanced(Commit headCommit,
                                                Commit otherCommit) {
        ArrayDeque<String> commits = new ArrayDeque<>();
        HashSet<String> ancestors = new HashSet<>();

        String otherCommitName = Utils.sha1(Utils.serialize(otherCommit));
        commits.add(otherCommitName);
        while (!commits.isEmpty()) {
            String currCommit = commits.remove();
            ancestors.add(currCommit);
            File currCommitFile = new File(COMMITS + "/" + currCommit);
            Commit commit = Utils.readObject(currCommitFile, Commit.class);
            if (commit.getParent() != null) {
                String parentName = Utils.sha1(
                        Utils.serialize(commit.getParent()));
                commits.add(parentName);
            }
            if (commit.getMergeParent() != null) {
                String mergeParentName = Utils.sha1(
                        Utils.serialize(commit.getMergeParent()));
                commits.add(mergeParentName);
            }
        }

        String headCommitName = Utils.sha1(Utils.serialize(headCommit));
        commits.add(headCommitName);
        while (!commits.isEmpty()) {
            String currCommit = commits.remove();
            File currCommitFile = new File(COMMITS + "/" + currCommit);
            Commit commit = Utils.readObject(currCommitFile, Commit.class);
            if (ancestors.contains(currCommit)) {
                return commit;
            }
            if (commit.getParent() != null) {
                String parentName = Utils.sha1(
                        Utils.serialize(commit.getParent()));
                commits.add(parentName);
            }
            if (commit.getMergeParent() != null) {
                String mergeParentName = Utils.sha1(
                        Utils.serialize(commit.getMergeParent()));
                commits.add(mergeParentName);
            }
        }
        return getCommit();
    }

    public static String abbreviatedUID(String commit) {
        File dir = new File(CWD + "/.gitlet/commits");
        List<String> files = Utils.plainFilenamesIn(dir);
        for (String filename : files) {
            if (commit.length() < filename.length()) {
                if (filename.startsWith(commit)) {
                    commit = filename;
                }
            }
        }
        return commit;
    }

    public static void overwriteConflict(String filename,
                                         TreeMap<String, String> currentCommit,
                                         TreeMap<String, String> givenCommit) {
        File newFile = new File(CWD + "/" + filename);
        String currentContents;
        String givenContents;
        if (currentCommit.get(filename).equals("")) {
            currentContents = "";
        } else {
            currentContents = Utils.readContentsAsString(new File(CWD
                    + "/.gitlet/blobs/"
                    + currentCommit.get(filename) + ".txt"));
        }

        if (givenCommit.get(filename).equals("")) {
            givenContents = "";
        } else {
            givenContents = Utils.readContentsAsString(new File(CWD
                    + "/.gitlet/blobs/" + givenCommit.get(filename) + ".txt"));
        }

        String line1 = "<<<<<<< HEAD\n";
        String line2 = currentContents;
        String line3 = "=======\n";
        String line4 = givenContents;
        String line5 = ">>>>>>>\n";
        String newContents;
        if (line2.equals("")) {
            newContents = line1 + line3 + line4 + line5;
        } else if (line4.equals("")) {
            newContents = line1 + line2 + line3 + line5;
        } else {
            newContents = line1 + line2 + line3 + line4 + line5;
        }
        Utils.writeContents(newFile, newContents);
        String newBlob = Utils.sha1(Utils.readContentsAsString(newFile));
        File newBlobFile = new File(CWD + "/.gitlet/blobs/" + newBlob + ".txt");
        try {
            newBlobFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
