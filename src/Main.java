import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;


import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Main {
    private static Map<String, Node> extern = null;

    public static void main(String[] args) {
        // todo put your path to directory here, eg: "/Users/uname/Documents/codeDir"
        String rootPath = null;
        // todo put the path you want to save the json at, eg. "/Users/uname/Documents/nodes.json"
        String savePath = null;
        // todo put the path you want to save the json at, eg. "/Users/uname/Documents/nodesExtern.json"
        String savePathExtern = null;
        if (rootPath == null || savePath == null || savePathExtern == null) return;

        Map<String, Node> nodeMap = runFromPath(rootPath);

        int count = 0;
        for (String path : new String[]{savePath, savePathExtern}) {
            try {
                FileWriter fw = new FileWriter(path);
                fw.write("[");
                boolean start = true;
                (count == 0 ? nodeMap : extern).forEach(new BiConsumer<String, Node>() {
                    @Override
                    public void accept(String s, Node node) {
                        try {
                            fw.write(",\n" + node.toJson());
                        } catch (IOException ex) {
                            System.out.println("failed to write node: " + node.getId());
                        }
                    }
                });
                fw.write(("]"));
                fw.close();
            } catch (IOException ex) {
                System.out.println("invalid out file: " + path);
                System.out.println("ex message: " + ex.getMessage());
            }

            try {
                File bufferFile = File.createTempFile("buffer", ".buff");
                FileWriter fw = new FileWriter(bufferFile);
                FileReader fr = new FileReader(path);
                BufferedReader br = new BufferedReader(fr);
                boolean first = true;
                while (br.ready()) {
                    String line = br.readLine();
                    if (first) line = line.substring(0, 1);
                    fw.write(line + "\n");
                    first = false;
                }
                fw.close();
                br.close();
                fr.close();
                File f = new File(path);
                bufferFile.renameTo(f);
            } catch (IOException ex) {
                System.out.println("failed to rewrite file");
            }
            count++;
        }

        return;
    }

    public static Map<String, Node> runFromPath(String path) {
        File file = new File(path);
        Map<String, Node> nodes = new HashMap<String, Node>();

        System.out.println("[running] getting all files");
        runFromPath(file, nodes, null);

        System.out.println("[running] converting paths to be relative");
        // make all paths relative
        String rootPath = file.getParentFile().getPath();
        nodes.forEach(new BiConsumer<String, Node>() {
            @Override
            public void accept(String s, Node node) {
                node.setDesc(node.getDesc().replace(rootPath, ""));
            }
        });

        System.out.println("[running] changing imports to ids");
        // change requires from package names to ids
        Map<String, Node> packageNames = new HashMap<>();
        extern = new HashMap<>();
        nodes.forEach(new BiConsumer<String, Node>() {
            @Override
            public void accept(String s, Node node) {
                packageNames.put(node.getPackageName(), node);
            }
        });
        nodes.forEach(new BiConsumer<String, Node>() {
            @Override
            public void accept(String s, Node node) {
                if (node.getRequires() == null) return;
                List<String> requires = node.getRequires();
                for (int i = 0; i < requires.size(); i++) {
                    Node n = packageNames.get(requires.get(i));
                    boolean wasNull = n == null;
                    if (wasNull) {
                        n = extern.get(requires.get(i));
                        if (n == null) {
                            n = new Node("ext");
                            n.setRequiredBy(new ArrayList<>());
                            n.setName(requires.get(i));
                            extern.put(n.getName(), n);
                        }
                    }
                    (wasNull ? extern : packageNames).get(requires.get(i)).getRequiredBy().add(node.getId());

                    requires.set(i, n.getId());
                }
            }
        });

        return nodes;
    }

    private static void runFromPath(File file, Map<String, Node> nodes, Node parentNode) {
        Node n = new Node(file.isDirectory() ? "dir" : "fil");
        n.setName(file.getName());
        if (!file.isDirectory()) {
            int charPeriod = file.getPath().lastIndexOf('.');
            int charSlash = file.getPath().lastIndexOf('/');
            int chatBackSlash = file.getPath().lastIndexOf('\\');
            charSlash = Math.max(charSlash, chatBackSlash);
            if (charPeriod != -1 && charPeriod > charSlash) {
                String ext = file.getPath().substring(charPeriod + 1);
                if (!ext.equals("java")) return;
                n.setName(n.getName().substring(0, n.getName().length() - (ext.length() + 1)));
            } else return;
        } else {
            if (file.getName().startsWith(".")) return;
        }

        if (parentNode != null) {
            n.setParent(parentNode.getId());
            if (parentNode.getRequiredBy() == null) {
                parentNode.setRequiredBy(new ArrayList<String>());
            }
            parentNode.getRequiredBy().add(n.getId());
        }
        n.setDesc(file.getPath());
        nodes.put(n.getDesc(), n);
        n.setRequires(new ArrayList<>());
        n.setRequiredBy(new ArrayList<>());
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                runFromPath(f, nodes, n);
            }
        } else {
            fillOutClassNode(n, nodes);
        }
    }

    private static void fillOutClassNode(Node node, Map<String, Node> nodes) {
        File file = new File(node.getDesc());

        if (node.getRequires() == null) node.setRequires(new ArrayList<String>());
        List<String> requires = getClassImportsNodeIds(file, node, nodes);

        if (requires != null) node.getRequires().addAll(requires);
    }

    private static List<String> getClassImportsNodeIds(File file, Node node, Map<String, Node> nodes) {
        try {
            CompilationUnit res = StaticJavaParser.parse(file);
            res.getPackageDeclaration().ifPresent(new Consumer<PackageDeclaration>() {
                @Override
                public void accept(PackageDeclaration packageDeclaration) {
                    String pkgName = packageDeclaration.getNameAsString();
                    if (node.getName() == "KeyAlreadyClonedException")
                        System.out.println("\t" + pkgName + "\n\t" + packageDeclaration.getName());
                    node.setPackageName(pkgName + "." + node.getName());
                }
            });
            NodeList<ImportDeclaration> imports = res.getImports();

            ArrayList<String> arr = new ArrayList<>();
            for (ImportDeclaration im : imports) {
                arr.add(im.getName().asString());
            }
            return arr;
        } catch (FileNotFoundException ex) {
            System.out.println("found but not found file: " + file.getPath());
        } catch (Exception e) {
            System.out.println("exception, file: " + file.getPath());
            System.out.println("\t" + e.getMessage());
        }
        return null;
    }
}
