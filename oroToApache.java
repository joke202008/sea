
package tools;

import java.io.*;
import org.apache.oro.text.regex.*;


public final class oroToApache {
  public static final String PACKAGE_PATTERN = "com\\.oroinc\\.(io|text|util)";
  public static final String PACKAGE_SUBSTITUTION = "org.apache.oro.$1";
  public static final String OLD_FILE_EXTENSION   = "_old";

  public static final class RenameException extends IOException {
    public RenameException() { }

    public RenameException(String message) {
      super(message);
    }
  }

  public static final class Converter {
    Pattern _sourcePattern;
    Perl5Matcher _matcher;
    Perl5Substitution _substitution;

    public static final int readFully(Reader reader, char[] buffer)
      throws IOException
    {
      int offset, length, charsRead;

      offset = 0;
      length = buffer.length;

      while(offset < buffer.length) {
	charsRead = reader.read(buffer, offset, length);
	if(charsRead == -1)
	  break;
	offset+=charsRead;
	length-=charsRead;
      }

      return offset;
    }

    public Converter(String patternString) throws MalformedPatternException {
      Perl5Compiler compiler;

      _matcher = new Perl5Matcher();
      compiler = new Perl5Compiler();
      _sourcePattern = compiler.compile(patternString);
      _substitution = new Perl5Substitution(PACKAGE_SUBSTITUTION);
    }

    public void convertFile(String filename, String oldExtension)
      throws FileNotFoundException, RenameException, SecurityException, 
	     IOException
    {
      char[] inputBuffer;
      int inputLength;
      File srcFile, outputFile;
      FileReader input;
      FileWriter output;
      String outputData;

      srcFile    = new File(filename);
      input      = new FileReader(srcFile);
      outputFile = 
	File.createTempFile(srcFile.getName(), null,
			    srcFile.getAbsoluteFile().getParentFile());
      output = new FileWriter(outputFile);

      inputBuffer = new char[(int)srcFile.length()];

      inputLength = readFully(input, inputBuffer);
      input.close();

      // new String(inputBuffer) is terribly inefficient because the
      // string ultimately gets converted back to a char[], but if we've
      // got the memory it's expedient.
      outputData = 
	Util.substitute(_matcher, _sourcePattern, _substitution,
			new String(inputBuffer), Util.SUBSTITUTE_ALL);
      output.write(outputData);
      output.close();

      if(!srcFile.renameTo(new File(srcFile.getAbsolutePath() +
				    OLD_FILE_EXTENSION)))
	throw new RenameException("Could not rename " + srcFile.getPath() +
				  ".");

      if(!outputFile.renameTo(srcFile))
	throw new RenameException("Could not rename temporary output file.  " +
				  "Original file is in " +
				  srcFile.getAbsolutePath() +
				  OLD_FILE_EXTENSION);
    }
  }

  public static final void main(String[] args) {
    int file;
    Converter converter;

    if(args.length < 1) {
      System.err.println("usage: oroToApache [file ...]");
      return;
    }

    try {
      converter = new Converter(PACKAGE_PATTERN);
    } catch(MalformedPatternException mpe) {
      // Shouldn''t happen
      mpe.printStackTrace();
      return;
    }

    for(file = 0; file < args.length; file++) {
      try {
	System.out.println("Converting " + args[file]);
	converter.convertFile(args[file], OLD_FILE_EXTENSION);
      } catch(FileNotFoundException fnfe) {
	System.err.println("Error: Could not open file.  Skipping " +
			   args[file]);
      } catch(RenameException re) {
	System.err.println("Error: " + re.getMessage());
      } catch(SecurityException se) {
	System.err.println("Error: Could not rename a file while processing" +
			   args[file] + ".  Insufficient permission.  " +
			   "File may not have been converted.");
      } catch(IOException ioe) {
	ioe.printStackTrace();
	System.err.println("Error: I/O exception while converting " +
			   args[file] + ".  File not converted.");
      }
    }
  }
}
