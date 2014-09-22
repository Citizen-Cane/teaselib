package teaselib;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

// Pasted from
// http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
// forums.devx.com/showthread.php?t=153784
// and changed to suit TeaseLib

/**
 * list resources available from the class path @ *
 */
public class ResourceList {

	/**
	 * for all elements of java.class.path get a Collection of resources Pattern
	 * pattern = Pattern.compile(".*"); gets all resources
	 * 
	 * @param pattern
	 *            the pattern to match
	 * @return the resources in the order they are found
	 */
	public static Collection<String> getResources(
			final URI element,
			final Pattern pattern) {
		final ArrayList<String> retval = new ArrayList<>();
		final File file = new File(element.getPath());
		if(file.isDirectory()){
			retval.addAll(getResourcesFromDirectory(file, pattern));
		} else{
			retval.addAll(getResourcesFromJarFile(file, pattern));
		}
		return retval;
	}

	private static Collection<String> getResourcesFromJarFile(
			final File file,
			final Pattern pattern){
		final ArrayList<String> retval = new ArrayList<String>();
		ZipFile zf;
		try{
			zf = new ZipFile(file);
		} catch(final ZipException e){
			throw new Error(e);
		} catch(final IOException e){
			throw new Error(e);
		}
		final Enumeration<? extends ZipEntry> e = zf.entries();
		while(e.hasMoreElements()){
			final ZipEntry ze = e.nextElement();
			final String fileName = ze.getName();
			final boolean accept = pattern.matcher(fileName).matches();
			if(accept){
				retval.add(fileName);
			}
		}
		try{
			zf.close();
		} catch(final IOException e1){
			throw new Error(e1);
		}
		return retval;
	}

	private static Collection<String> getResourcesFromDirectory(
			final File directory,
			final Pattern pattern){
		final ArrayList<String> retval = new ArrayList<String>();
		final File[] fileList = directory.listFiles();
		for(final File file : fileList){
			if(file.isDirectory()){
				retval.addAll(getResourcesFromDirectory(file, pattern));
			} else{
				try{
					final String fileName = file.getCanonicalPath();
					final boolean accept = pattern.matcher(fileName).matches();
					if(accept){
						retval.add(fileName);
					}
				} catch(final IOException e){
					throw new Error(e);
				}
			}
		}
		return retval;
	}
}  
