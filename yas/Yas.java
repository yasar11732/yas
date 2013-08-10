package yas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

class TreeEntry {

    public String perms;
    public String type;
    public String hash;
    public String name;

    @Override
    public String toString() {
        return perms + " " + type + " " + hash + " " + name;
    }
}

public class Yas {

    private static File userdir = new File(System.getProperty("user.dir"));
    private static File basedir = null;

    public static void main(String[] args) {
        //catFile("91f323f188d51196bd643216f37b4de1a6459114");
        //parseCommit("58ee77447a6c472413ce91fcfca8d25ffe33ff45");
        //resetTree("e1c8ba06ffd41c9ea147faa0f2800a4f64f36d5d", userdir);
        //System.out.println(readObject("e1c8ba06ffd41c9ea147faa0f2800a4f64f36d5d"));
        //return;
        if (args.length == 0) {
            System.out.println("Please provide an argument");
            return;
        }

        if (args[0].equals("init")) {
            File dir = new File(userdir, ".yas");
            if (!dir.exists() && !dir.mkdir()) {
                System.err.println("Couldn't create " + dir);
            }
            return;
        }

        setBaseDir();
        if (basedir == null) {
            System.out.println("Please initialize a object database with 'yas init' command");
            return;
        }
        switch (args[0]) {
            case "commit":
                if (args.length == 1) {
                    System.err.println("Please provide a message like this: ");
                    System.err.println("Yas commit 'first commit'");
                    return;
                }
                commit(args[1]);
                break;
            case "log":
                log();
                break;
            case "cat-file":
                if (args.length == 1) {
                    System.err.println("Please provide hash of the file to see");
                    return;
                }
                catFile(args[1]);
                break;
            case "reset":
                if (args.length > 1) {
                    reset(args[1]);
                } else {
                    try {
                        reset(getLastCommit());
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                    }
                }
                break;


        }
    }

    public static void reset(String hash) {

        if (hash == null) { // use last commit
            try {
                hash = getLastCommit();
            } catch (IOException e) {
                System.err.println("Exception occured while trying to get last commit: " + e.getMessage());
            }
        }
        HashMap commit = parseCommit(hash);
        if(resetTree((String) commit.get("tree"))) {
            writeToFile(new File(new File(userdir, ".yas"), "HEAD"), hash);
        }
    }

    private static List<TreeEntry> parseTree(String hash) {
        String tree = readObject(hash);


        if (!tree.startsWith("tree")) {
            System.err.println("Given hash doesn't belong to a tree object");
            return null;
        }

        List<TreeEntry> entries = new LinkedList<>();

        int nullCharacterPosition = tree.indexOf("\0");
        tree = tree.substring(nullCharacterPosition + 1);
        String[] dataTree = tree.split("\n");
        for (String entry : dataTree) {
            TreeEntry e = new TreeEntry();
            String[] args = entry.split(" ");
            e.perms = args[0];
            e.type = args[1];
            e.hash = args[2];
            e.name = args[3];

            // account for spaces in name
            int i = 4;
            while (i < args.length) {
                e.name += " " + args[i];
                i++;
            }

            entries.add(e);
        }
        return entries;
    }

    public static boolean resetTree(String hash) {

        List<TreeEntry> entries = parseTree(hash);
        if (entries == null) {
            return false;
        }

        for (TreeEntry e : entries) {
            boolean retval = false;
            switch (e.type) {
                case "blob":
                    retval = resetFile(e.hash, e.name);
                    break;
                case "tree":
                    retval = resetTree(e.hash);
                    break;
            }
            if (!retval) {
                System.err.println("Couldn't restore" + e.name);
                return false;
            }
        }

        return true;

    }

    public static boolean resetFile(String hash, String name) {
        File target = new File(basedir, name);
        System.out.println("resetting " + target);
        
        /* uncomment for debugging
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        for (StackTraceElement s: st) {
            System.out.println(s);
        } */
        
        String content = readObject(hash);
        try (BufferedWriter wr = new BufferedWriter(new FileWriter(target.getName()))) {
            int nullchar = content.indexOf("\0");
            wr.write(content.substring(nullchar + 1));
            return true;
        } catch (IOException e) {
            System.err.println("An error occured while writing to file: " + e.getMessage());
            return false;
        }
    }

    public static void log() {
        String commit;
        try {
            commit = getLastCommit();
        } catch (IOException e) {
            System.err.println("Exception occured while trying to get last commit: " + e.getMessage());
            return;
        }
        HashMap<String, String> h;

        while (commit != null) {
            h = parseCommit(commit);
            System.out.println(h.get("datetime") + " " + commit + " " + h.get("message"));
            commit = h.get("parent");
        }

    }

    public static String getLastCommit() throws IOException {
        try {
            return new String(readFile(new File(new File(userdir, ".yas"), "HEAD").getCanonicalPath()));
        } catch (FileNotFoundException e) {
            return null; // nothing committed yet
        }
    }

    private static void setBaseDir() {
        File current = userdir;
        while (current != null && basedir == null) {
            String[] ls = current.list();
            for (String f : ls) {
                if (f.equals(".yas")) {
                    if (new File(current, f).isDirectory()) {
                        basedir = current;
                        break;
                    }
                }
            }
            current = current.getParentFile();
        }

    }

    public static File getObjectFile(String sha) {
        return new File(new File(new File(new File(userdir, ".yas"), "objects"), sha.substring(0, 2)), sha.substring(2));
    }

    public static void writeToFile(File file, String content) {
        try {
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(content);
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static HashMap<String, String> parseCommit(String hash) {
        HashMap<String, String> d = new HashMap<>();
        String commit = readObject(hash);
        if (!commit.startsWith("commit ")) {
            System.out.println("This is not a valid commit object" + hash);
            return null;
        }
        
        // skip header part
        int nullCharPos = commit.indexOf("\0");
        commit = commit.substring(nullCharPos + 1);
        
        String[] entries = commit.split("\n");
        for(String entry: entries) {
            String[] cols = entry.split(" ");
            int i = 2;
            String value = cols[1];
            while (i < cols.length) {
                value += " " + cols[i];
                i++;
            }
            d.put(cols[0], value);
        }
        return d;
    }

    public static String commit(String message) {
        String hashString;
        String currentHead = null;
        String tree = hashTree(userdir); // sha1 of current tree

        try {
            currentHead = new String(readFile(new File(new File(userdir, ".yas"), "HEAD").getCanonicalPath()));
        } catch (FileNotFoundException e) {
            // first commit
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
        System.out.println("current head :" + currentHead);
        if (currentHead != null) {
            HashMap<String, String> lastCommit = parseCommit(currentHead);
            if (tree.equals(lastCommit.get("tree"))) {
                System.out.println("Nothing to commit");
                return currentHead;
            }
        }



        StringBuilder sb = new StringBuilder();
        if (currentHead != null) {
            sb.append("parent ");
            sb.append(currentHead);
            sb.append("\n");
        }
        sb.append("tree ");
        sb.append(tree);
        sb.append("\n");
        // see: http://www.w3.org/TR/NOTE-datetime
        SimpleDateFormat df = new SimpleDateFormat("Y-MM-dd'T'H:m:ssZ");
        sb.append("datetime " + df.format(new Date()) + "\n");
        sb.append("message " + message);
        sb.insert(0, "commit " + sb.length() + '\0');
        byte[] input = sb.toString().getBytes();
        try {
            byte[] sha1 = getSha1(input);
            hashString = shaToString(sha1);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Couldn't calculate sha1: " + e.getMessage());
            return null;
        }
        System.out.println("Current head is now: " + hashString);
        File destFile = prepareDestFile(hashString);

        if (destFile != null) {
            if (destFile.exists()) {
                writeToFile(new File(new File(userdir, ".yas"), "HEAD"), hashString);
                return hashString;
            }

            if (writeZippedFile(destFile, input)) {
                writeToFile(new File(new File(userdir, ".yas"), "HEAD"), hashString);
                return hashString;
            } else {
                return null;
            }
        } else {
            return null;
        }


        /*
         try {
         File temp = File.createTempFile("commit-", ".tmp");
         if (System.getProperty("os.name").toLowerCase().contains("windows")) {
         String cmd = "rundll32 url.dll,FileProtocolHandler " + temp.getCanonicalPath();
         Process p = Runtime.getRuntime().exec(cmd);
         p.waitFor();
         System.out.println("Reached this line");
         } else {
         Desktop.getDesktop().edit(temp);
         }
         } catch (IOException| InterruptedException e) {
         System.err.println(e.getMessage());
         } */

    }

    /**
     * Given the hash, it pretty prints object content
     *
     * @param hash
     */
    public static void catFile(String hash) {
        File destFile = prepareDestFile(hash);
        try (InputStream is = new InflaterInputStream(new FileInputStream(destFile))) {

            // read the stream a single byte each time until we encounter '\0'
            int aByte;
            while ((aByte = is.read()) != -1) {
                if (aByte == '\0') {
                    break;
                }
            }
            try (BufferedReader b = new BufferedReader(new InputStreamReader(is))) {
                int i;
                while ((i = b.read()) != -1) {
                    System.out.print((char) i);
                }
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static String readObject(String hash) {
        File destFile = getObjectFile(hash);
        try (InputStream is = new InflaterInputStream(new FileInputStream(destFile))) {
            StringBuilder sb = new StringBuilder();

            try (BufferedReader b = new BufferedReader(new InputStreamReader(is))) {
                int i;
                while ((i = b.read()) != -1) {
                    sb.append((char) i);
                }
            }
            return sb.toString();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public static byte[] getSha1(byte[] arr) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        return mDigest.digest(arr);
    }

    public static byte[] readFile(String fileName) throws FileNotFoundException, IOException {
        RandomAccessFile f = new RandomAccessFile(fileName, "r");
        byte[] b = new byte[(int) f.length()];
        f.read(b);
        return b;
    }

    public static File prepareDestFile(String hashString) {
        File yasDir = new File(System.getProperty("user.dir"), ".yas");
        File objectDir = new File(yasDir, "objects");
        File destDir = new File(objectDir, hashString.substring(0, 2));

        if (!destDir.exists() && !destDir.mkdirs()) {
            System.err.println("Couldn't create target directory");
            return null;
        }

        return new File(destDir, hashString.substring(2));
    }

    public static String shaToString(byte[] arr) {
        StringBuilder sb = new StringBuilder();
        for (byte aResult : arr) {
            sb.append(Integer.toString((aResult & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static boolean writeZippedFile(File destFile, byte[] input) {
        try {
            if (!destFile.exists() && !destFile.createNewFile()) {
                System.out.println("Couldn't create target file:" + destFile);
                return false;
            }
            try (OutputStream out = new DeflaterOutputStream(new FileOutputStream(destFile))) {
                out.write(input);
            }
            return true;

        } catch (IOException e) {
            System.err.println("An I/O error occured: " + e.getMessage());
            return false;
        }
    }

    private static String hashTree(File path) {

        String hashString;
        if (!path.isDirectory()) {
            System.err.println(path + " is not a directory");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        String[] contents = path.list();
        for (String content : contents) {
            // ignore hidden folders
            if (content.substring(0, 1).equals(".")) {
                continue;
            }

            File f = new File(path, content);
            TreeEntry entry = new TreeEntry();
            entry.name = basedir.toURI().relativize(f.toURI()).getPath();
            if (f.isDirectory()) {
                entry.hash = hashTree(f);
                if (entry.hash == null) {
                    return null;
                }
                entry.perms = "040000";
                entry.type = "tree";
            } else {
                entry.hash = hashBlob(f);
                if (entry.hash == null) {
                    return null;
                }
                entry.type = "blob";
                if (f.canExecute()) {
                    entry.perms = "100755";
                } else {
                    entry.perms = "100644";
                }
            }
            sb.append(entry);
            sb.append("\n");
        }

        // remove extra newline from the end
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }

        sb.insert(0, "tree " + sb.length() + '\0');
        byte[] input = sb.toString().getBytes();
        try {
            byte[] sha1 = getSha1(input);
            hashString = shaToString(sha1);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Couldn't calculate sha1: " + e.getMessage());
            return null;
        }

        File destFile = prepareDestFile(hashString);

        if (destFile != null) {
            if (destFile.exists()) {
                return hashString;
            }

            if (writeZippedFile(destFile, input)) {
                return hashString;
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    private static String hashBlob(File file) {
        /**
         * read from file and create a hash for file then write contents of the
         * file into a compressed file
         *
         * @param file
         * @return
         */
        String hashString; // hold sha1 hash
        String fileContents = null;
        try {
            fileContents = new String(readFile(file.toString()));
        } catch (IOException e) {
            System.err.println("Couldn't read " + file + ": " + e.getMessage());
        }

        // The part appended to content shows what kind of content
        // this is (blob) and it's size.
        byte[] input = ("blob " + fileContents.length() + '\0' + fileContents).getBytes();

        // we don't need it anymore, let gc delete it.
        fileContents = null;

        try {
            byte[] sha1 = getSha1(input);
            hashString = shaToString(sha1);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Couldn't calculate sha1: " + e.getMessage());
            return null;
        }

        File destFile = prepareDestFile(hashString);

        if (destFile != null) {
            if (destFile.exists()) {
                return hashString;
            }

            if (writeZippedFile(destFile, input)) {
                return hashString;
            } else {
                return null;
            }
        } else {
            return null;
        }

    }
}
