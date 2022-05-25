package gitlet;

import java.io.Serializable;
import java.util.TreeMap;

public class Index implements Serializable {

    public Index() {
        toAdd = new TreeMap<>();
        toDelete = new TreeMap<>();
    }

    public TreeMap<String, String> getAdd() {
        return toAdd;
    }

    public TreeMap<String, String> getRemove() {
        return toDelete;
    }

    public void toStage(String filename, String blob) {
        toAdd.put(filename, blob);
    }

    public void toRemove(String filename, String blob) {
        toDelete.put(filename, blob);
    }

    public void reset() {
        toAdd.clear();
        toDelete.clear();
    }

    public boolean fileExistsToAdd(String filename) {
        if (toAdd.containsKey(filename)) {
            return true;
        }
        return false;
    }

    public boolean fileExistsToDelete(String filename) {
        if (toDelete.containsKey(filename)) {
            return true;
        }
        return false;
    }

    public boolean fileExists(String filename) {
        if (fileExistsToAdd(filename) || fileExistsToDelete(filename)) {
            return true;
        }
        return false;
    }

    public boolean empty() {
        if (toDelete.isEmpty() && toAdd.isEmpty()) {
            return true;
        }
        return false;
    }

    public void remove(String filename) {
        if (toAdd.containsKey(filename)) {
            toAdd.remove(filename);
        } else if (toDelete.containsKey(filename)) {
            toDelete.remove(filename);
        }
    }

    /** Files staged for addition. */
    private TreeMap<String, String> toAdd;

    /** Files staged for removal. */
    private TreeMap<String, String> toDelete;

}
