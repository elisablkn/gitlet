package gitlet;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;


public class Commit implements Serializable {

    /** Log message. */
    private String _message;

    /** Timestamp of the commit. */
    private String _timestamp;

    /** Head commit parent. */
    private Commit _parent;

    /** Merge parent. */
    private Commit _mergeParent;

    /** The TreeMap containing the filenames and the
     * corresponding blobs of the files tracked in the commit. */
    private TreeMap<String, String> _contents;

    public Commit(String message, Commit parent,
                  String timestamp, Commit parent2) {
        _message = message;
        _parent = parent;
        _timestamp = timestamp;
        _contents = new TreeMap<>();
        _mergeParent = parent2;
    }

    public Commit getParent() {
        return _parent;
    }

    public String getMessage() {
        return _message;
    }

    public String getTimestamp() {
        return _timestamp;
    }

    public Commit getMergeParent() {
        return _mergeParent;
    }

    public TreeMap<String, String> getContents() {
        return _contents;
    }

    public void addToCommit(String filename, String blob) {
        _contents.put(filename, blob);
    }

    public String getBlob(String filename) {
        if (_contents.containsKey(filename)) {
            return _contents.get(filename);
        }
        return "";
    }

    public void setContents(TreeMap<String, String> files) {
        _contents = files;
    }

    public String getDate(String timestamp1)
            throws ParseException {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = dateTime.parse(timestamp1);
        cal.setTime(date);

        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime ldt = LocalDateTime.parse(timestamp1,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String dayWeek = new SimpleDateFormat("EEE").format(date);
        String month = new SimpleDateFormat("MMM").format(cal.getTime());
        DateTimeFormatter format = DateTimeFormatter.
                ofPattern("yyyy-MM-dd HH:mm:ss");
        timestamp1 = format.format(ldt.atZone(zone));

        String day;
        if (timestamp1.charAt(8) == '0') {
            day = String.valueOf(timestamp1.charAt(9));
        } else {
            day = timestamp1.substring(8, 10);
        }

        String time = timestamp1.substring(11);
        String year = timestamp1.substring(0, 4);
        String result = dayWeek + " " + month
                + " " + day + " " + time
                + " " + year + " -0800";
        return result;
    }

    public void printCommit(String name) throws ParseException {
        System.out.println("===");
        System.out.println("commit " + name);
        System.out.println("Date: " + getDate(_timestamp));
        System.out.println(_message);
        System.out.println();
    }

}
