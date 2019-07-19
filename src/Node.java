import java.util.List;

public class Node {
    public static void main(String[] args) {
        Node n = new Node();
        for (int i = 0; i < 55; i++) {
            System.out.println(n.incrementId());
        }
    }

    private static String idCount = "";
    private String idPrefix;

    private String id;
    private List<String> requires;
    private List<String> requiredBy;
    private String parent;
    private String name;
    private String desc;
    private String packageName;

    public Node() {
        this.idPrefix = "aaa";
        this.id = this.incrementId();
        this.requires = null;
        this.requiredBy = null;
        this.parent = null;
        this.name = null;
        this.desc = null;
    }

    public Node(String prefix) {
        this.idPrefix = prefix;
        this.id = this.incrementId();
        this.requires = null;
        this.requiredBy = null;
        this.parent = null;
        this.name = null;
        this.desc = null;
    }

    public String getId() {
        return this.idPrefix + this.id;
    }

    public List<String> getRequires() {
        return requires;
    }

    public void setRequires(List<String> requires) {
        this.requires = requires;
    }

    public List<String> getRequiredBy() {
        return requiredBy;
    }

    public void setRequiredBy(List<String> requiredBy) {
        this.requiredBy = requiredBy;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String toJson() {
        String ret = "{";
        ret += "\"id\":\"" + this.getId() + "\"";
        ret += ",\"name\":\"" + this.getName() + "\"";
        ret += ",\"desc\":\"" + this.getDesc() + "\"";
        ret += ",\"parent\":\"" + this.getParent() + "\"";
//        ret += ",\"package\":\"" + this.getPackageName() + "\"";
        String arr = "";
        if (this.getRequires() != null) {
            arr = "[";
            boolean first = true;
            for (String e : this.getRequires()) {
                arr += (first ? "" : ",") + "\"" + e + "\"";
                first = false;
            }
            arr += "]";
            ret += ",\"requires\":" + arr;
        }
        if (this.getRequiredBy() != null) {
            arr = "[";
            boolean first = true;
            for (String e : this.getRequiredBy()) {
                arr += (first ? "" : ",") + "\"" + e + "\"";
                first = false;
            }
            arr += "]";
            ret += ",\"requiredBy\":" + arr;
        }
        ret += "}";

        return ret;
    }

    private String incrementId() {
        String letters = "abcdefghijklmnopqrstuvwxyz";
        String lettersAndCap = letters + letters.toUpperCase();
        String lettersAndCapAndNum = "0123456789" + lettersAndCap;
        boolean carry = Node.idCount.length() == 0;
        for (int i = Node.idCount.length() - 1; i >= 0; i--) {
            char c = Node.idCount.charAt(i);
            String toUse = i == 0 ? lettersAndCap : lettersAndCapAndNum;
            if (toUse.indexOf(c) == toUse.length() - 1) { // carry over
                c = carry ? 'b' : 'a';
                carry = true;
            } else if (carry && toUse.indexOf(c) == toUse.length() - 2) {
                c = 'a';
            } else {
                c = toUse.charAt(toUse.indexOf(c) + (carry ? 2 : 1));
                carry = false;
            }
            Node.idCount = Node.idCount.substring(0, i) + c + Node.idCount.substring(i + 1);
            if (!carry) break;
        }
        if (carry) {
            Node.idCount = "a" + Node.idCount;
        }
        return Node.idCount;
    }
}
