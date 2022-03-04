import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {

        if (args.length != 3) {
            throw new IllegalArgumentException("Expected three strings, <language> <fileName> <className>");
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        String fileTxt = Files.readString(Path.of("src/" + args[1]));
        Translator translator = new Translator(args[0]);
        fileTxt = translator.translate(fileTxt);

        JavaFileObject file = new JavaSourceFromString(args[2], fileTxt);

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);

        boolean success = task.call();

        // if something went wrong lets log to the user
        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
            System.out.println(diagnostic.getCode());
            System.out.println(diagnostic.getKind());
            System.out.println(diagnostic.getPosition());
            System.out.println(diagnostic.getStartPosition());
            System.out.println(diagnostic.getEndPosition());
            System.out.println(diagnostic.getSource());
            System.out.println(diagnostic.getMessage(null));
        }

        if (success) {
            try {
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { new File("").toURI().toURL() });
                Class.forName(args[2], true, classLoader).getDeclaredMethod("main", new Class[] { String[].class }).invoke(null, new Object[] { null });
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found: " + e);
            } catch (NoSuchMethodException e) {
                System.err.println("No such method: " + e);
            } catch (IllegalAccessException e) {
                System.err.println("Illegal access: " + e);
            } catch (InvocationTargetException e) {
                System.err.println("Invocation target: " + e);
            }
        }
    }
}

class Translator {

    private HashMap<String, String> keyWords;

    public Translator(String language) {
        this.keyWords = new HashMap<>();
        fillMap(language);
    }

    public void fillMap(String language) {

        if (language.equalsIgnoreCase("spanish") || language.equalsIgnoreCase("español")) {
            keyWords.put("verda", "true");
            keyWords.put("falso", "false");
            keyWords.put("nuevo", "new");
            keyWords.put("si", "if");
            keyWords.put("ent", "int");
            keyWords.put("publico", "public");
            keyWords.put("estatico", "static");
            keyWords.put("clase", "class");
            keyWords.put("letra", "char");
            keyWords.put("imprimir", "System.out.println");
            keyWords.put("Texto", "String");
            keyWords.put("nuevo", "new");
            keyWords.put("vacio", "void");
            keyWords.put("volver", "return");
            keyWords.put("mientras", "while");
        }
    }

    public String translate(String str) {

        for (Map.Entry<String, String> keyWord : keyWords.entrySet()) {
            str = str.replaceAll("(\\W?)(" + keyWord.getKey() + ")(\\W)", "$1" + keyWord.getValue() + "$3");
        }

        return str;
    }
}

class JavaSourceFromString extends SimpleJavaFileObject {
    final String code;

    JavaSourceFromString(String name, String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}