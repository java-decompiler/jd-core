package org.jd.core.v1.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.Decompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.printer.Printer;
import org.jd.core.v1.impl.loader.DirectoryLoader;
import org.jd.core.v1.impl.printer.PlainTextPrinter;

public class CliMain {

	public final static String VERSION = "1.0 2021";

	public static void usage() {
		System.out.println("Usage:");
		System.out.println("  java org.jd.core.v1.cli [-h |-v| [options] <classes folder>]");
		System.out.println("Decompile all .class files inside given folder.");
		System.out.println("Actions:");
		System.out.println("    -h | --help                  print this help message then exit");
		System.out.println("    -v | --version               print version then exit");
		System.out.println("Options:");
		System.out.println("    -d | --dest-folder <path>    java files folder (default classes folder)");
		System.out.println("    --override                   override existing java files");
		System.out.println("    --escape-unicode             escape unidoce characters");
		System.out.println("    --no-line-numbers            omit line numbers");
	}

	public static void version() {
		System.out.println(VERSION);
	}

	public static void main(String[] args) {

		if (args.length == 0) {
			System.err.println("Missing arguments.");
			usage();
			System.exit(1);
		}

		if ("-h".equals(args[0]) || "--help".equals(args[0])) {
			usage();
			return;
		}
		if ("-v".equals(args[0]) || "--version".equals(args[0])) {
			version();
			return;
		}

		int i = 0; // current CLI argument
		File srcFolder = null;
		File destFolder = null;
		boolean override = false;
		boolean escapeUnicode = false;
		boolean printLineNumbers = true;

		while (i < args.length) {
			if ("-d".equals(args[i]) || "--dest-folder".equals(args[i])) {
				destFolder = new File(args[++i]);
			} else if ("--override".equals(args[i])) {
				override = true;
			} else if ("--escape-unicode".equals(args[i])) {
				escapeUnicode = true;
			} else if ("--no-line-numbers".equals(args[i])) {
				printLineNumbers = false;
			} else if (args[i].startsWith("-")) {
				System.err.println("Unknown option: " + args[i]);
				usage();
				System.exit(1);
			} else if (args.length - i > 1) {
				System.err.println("Too many arguments.");
				usage();
				System.exit(1);
			} else {
				srcFolder = new File(args[i]);
			}
			++i;
		}
		if (srcFolder == null || srcFolder.getPath().trim().isEmpty()) {
			System.err.println("Missing classes folder.");
			usage();
			System.exit(1);
		}
		if (destFolder == null) {
			destFolder = srcFolder;
		}

		prepareFolders(srcFolder, destFolder);

		// adding whole folder to classpath should give better decompilation, doesn't
		// it?
		addToClassPath(srcFolder);

		Loader loader = new DirectoryLoader(srcFolder);
		Printer printer = new PlainTextPrinter(escapeUnicode, printLineNumbers);
		Decompiler decompiler = new ClassFileToJavaSourceDecompiler();
		List<String> internalNames = listClasses(srcFolder);
		for (String internalName : internalNames) {
			try {
				decompiler.decompile(loader, printer, internalName);
			} catch (Exception e) {
				System.err.println("Exception while decompiling " + internalName + " : " + e.getMessage());
				continue;
			}
			try {
				writeFile(printer.toString(), destFolder, internalName, override);
			} catch (IOException e) {
				System.err.println("Exception while writing " + destFolder.getPath() + File.separator + internalName
						+ " : " + e.getMessage());
			}
		}
	}

	public static void prepareFolders(File srcFolder, File destFolder) {
		if (!srcFolder.exists()) {
			System.err.println("Classes folder does not exists.");
			System.exit(2);
		}
		if (!srcFolder.canRead()) {
			System.err.println("Classes folder is not readable.");
			System.exit(3);
		}
		if (!srcFolder.isDirectory()) {
			System.err.println("Classes folder is not a directory.");
			System.exit(4);
		}
		if (!destFolder.exists()) {
			destFolder.mkdirs();
		} else if (!destFolder.isDirectory()) {
			System.err.println("Destination folder is not a directory.");
			System.exit(5);
		}
		if (!destFolder.canWrite()) {
			System.err.println("Destination folder is not writable.");
			System.exit(6);
		}
	}

	/**
	 * Add a folder to classpath
	 * 
	 * @see https://stackoverflow.com/a/7884406/5116356
	 */
	@SuppressWarnings("deprecation")
	public static void addToClassPath(File folder) {
		URL u;
		try {
			u = folder.toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e); // can this happen ?!?
		}
		URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<URLClassLoader> urlClass = URLClassLoader.class;
		try {
			Method method = urlClass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(urlClassLoader, new Object[] { u });
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<String> listClasses(File srcFolder) {
		List<File> list = new ArrayList<>();
		listClasses(srcFolder.listFiles(), list);
		List<String> ls = list.stream() //
				.map(x -> getClassName(srcFolder, x)) //
				.collect(Collectors.toList());
		return ls;
	}

	public static String getClassName(File srcFolder, File classFile) {
		String s = classFile.getPath();
		final int beginIndex = srcFolder.getPath().length();
		final int difflen = ".class".length();
		s = s.substring(beginIndex, s.length() - difflen);
		if (s.startsWith("/")) {
			s = s.substring(1);
		}
		return s;
	}

	private static void listClasses(File[] files, List<File> result) {
		for (File file : files) {
			if (file.isDirectory()) {
				listClasses(file.listFiles(), result);
			} else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
				result.add(file);
			}
		}
	}

	public static void writeFile(String source, File destFolder, String internalName, boolean override)
			throws IOException {
		File f = new File(destFolder, internalName + ".java");
		if (f.exists() && !override) {
			System.err.println("Skipping existing file " + f.getPath());
			return;
		}
		try (FileWriter fw = new FileWriter(f)) {
			fw.write(source);
		}
	}
}
