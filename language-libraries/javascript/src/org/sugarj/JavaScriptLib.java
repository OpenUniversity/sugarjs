package org.sugarj;

// TODO: revisit as only PrologLib modified to compile

import static org.sugarj.common.ATermCommands.getApplicationSubterm;
import static org.sugarj.common.ATermCommands.isApplication;
import static org.sugarj.common.Log.log;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.stratego_gpp.parse_pptable_file_0_0;
import org.sugarj.common.ATermCommands;
import org.sugarj.common.Environment;
import org.sugarj.common.FileCommands;
import org.sugarj.common.IErrorLogger;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;
import org.sugarj.languagelib.SourceFileContent;
import org.sugarj.javascript.JavaScriptSourceFileContent;
import org.sugarj.javascript.JavaScriptSourceFileContent.JavaScriptModuleImport;

public class JavaScriptLib extends LanguageLib implements Serializable {

  private static final long serialVersionUID = -8431879767852508991L;

  private transient File libDir;
  
  private Set<RelativePath> generatedFiles = new HashSet<RelativePath>();

  private Path jsOutFile;

  private JavaScriptSourceFileContent jsSource;

  private String decName;
  private String relNamespaceName;
    
  private IStrategoTerm pptable = null;
  private File prettyPrint = null;

  public String getVersion() {
    return "javascript-0.1";
  }

  private File getPrettyPrint() {
    if (prettyPrint == null)
      prettyPrint = ensureFile("org/sugarj/languages/JavaScript.pp");
    
    return prettyPrint;
  }
    
  @Override
  public List<File> getGrammars() {
    List<File> grammars = new LinkedList<File>(super.getGrammars());
    grammars.add(ensureFile("org/sugarj/languages/SugarJS.def"));
    grammars.add(ensureFile("org/sugarj/languages/JavaScript.def"));
    return Collections.unmodifiableList(grammars);
  }
  
  @Override
  public File getInitGrammar() {
    return ensureFile("org/sugarj/javascript/init/initGrammar.sdf");
  }

  @Override
  public String getInitGrammarModule() {
    return "org/sugarj/javascript/init/initGrammar";
  }

  @Override
  public File getInitTrans() {
    return ensureFile("org/sugarj/javascript/init/InitTrans.str");
  }

  @Override
  public String getInitTransModule() {
    return "org/sugarj/javascript/init/InitTrans";
  }

  @Override
  public File getInitEditor() {
    return ensureFile("org/sugarj/javascript/init/initEditor.serv");
  }

  @Override
  public String getInitEditorModule() {
    return "org/sugarj/javascript/init/initEditor";
  }

  @Override
  public File getLibraryDirectory() {
    if (libDir == null) { // set up directories first
      String thisClassPath = "org/sugarj/JavaScriptLib.class";
      URL thisClassURL = JavaScriptLib.class.getClassLoader().getResource(thisClassPath);
      
      System.out.println(thisClassURL);
      
      if (thisClassURL.getProtocol().equals("bundleresource"))
        try {
          thisClassURL = FileLocator.resolve(thisClassURL);
        } catch (IOException e) {
          e.printStackTrace();
        }
      
      String classPath = thisClassURL.getPath();
      String binPath = classPath.substring(0, classPath.length() - thisClassPath.length());
      
      libDir = new File(binPath);
    }
    
    return libDir;
  }
  
    public static void main(String args[]) throws URISyntaxException {
    JavaScriptLib jsl = new JavaScriptLib();
    
    for (File file : jsl.getGrammars()) 
      exists(file);


      exists(jsl.getInitGrammar());
      exists(jsl.getInitTrans());
      exists(jsl.getInitEditor());
      exists(jsl.libDir);
    }
    
    private static void exists(File file) {
      if (file.exists())
        System.out.println(file.getPath() + " exists.");
      else
        System.err.println(file.getPath() + " does not exist.");
    }
    
    
    @Override
    public boolean isLanguageSpecificDec(IStrategoTerm decl) {
    return isApplication(decl, "NonUnitClause") || 
        isApplication(decl, "UnitClause") ||
        isApplication(decl, "Query") ||
        isApplication(decl, "Command") ||
        isApplication(decl, "ModuleReexport");        
    }

    @Override
    public boolean isSugarDec(IStrategoTerm decl) {
      return isApplication(decl, "SugarBody");           
    }
    
    @Override
    public boolean isNamespaceDec(IStrategoTerm decl) {
      return isApplication(decl, "ModuleDec") ||
          isApplication(decl, "SugarModuleDec");
    }
    
    @Override
    public boolean isEditorServiceDec(IStrategoTerm decl) {
      return isApplication(decl, "EditorServicesDec");   
    }

    @Override
    public boolean isImportDec(IStrategoTerm decl) {
      return isApplication(decl, "ModuleImport");
    }

    @Override
    public boolean isPlainDec(IStrategoTerm decl) {
      return isApplication(decl, "PlainDec");        
    }

  @Override
  public String getGeneratedFileExtension() {
    return "js";
  }

  @Override
  public String getSugarFileExtension() {
    return "sjs";
  }

  @Override
  public SourceFileContent getSource() {
    return jsSource;
  }

  @Override
  public Path getOutFile() {
    return jsOutFile;
  }

  @Override
  public Set<RelativePath> getGeneratedFiles() {
    return generatedFiles;
  }

  @Override
  public void processLanguageSpecific(IStrategoTerm toplevelDecl, Environment environment) throws IOException {
    jsSource.addBodyDecl(prettyPrint(toplevelDecl));
  }

  
  private IStrategoTerm initializePrettyPrinter(Context ctx) {
    if (pptable == null) {
      IStrategoTerm pptable_file = ATermCommands.makeString(getPrettyPrint().getAbsolutePath());
      pptable = parse_pptable_file_0_0.instance.invoke(org.strategoxt.stratego_gpp.stratego_gpp.init(), pptable_file);
    }
    
    return pptable;
  }
  
  @Override
  public String prettyPrint(IStrategoTerm term) throws IOException {
    IStrategoTerm ppTable = initializePrettyPrinter(interp.getCompiledContext());
    return ATermCommands.prettyPrint(ppTable, term, interp);
  }

  @Override
  public void setupSourceFile(RelativePath sourceFile, Environment environment) {
    jsOutFile = environment.createBinPath(FileCommands.dropExtension(sourceFile.getRelativePath()) + "." + getGeneratedFileExtension());
    jsSource = new JavaScriptSourceFileContent(this);
    jsSource.setOptionalImport(false);    
  }

  @Override
  public String getRelativeNamespace() {
    // XXX: Is there a namespace separator in prolog? Or even any notion of compound namespaces?
    // XXX: From swi prolog doc: Modules are organised in a single and flat namespace and therefore module names must be chosen with some care to avoid conflicts.
    // XXX: SugarProlog will implement different namespace handling.
    return relNamespaceName;
  } 
  
  @Override
  public void processNamespaceDec(IStrategoTerm toplevelDecl,
      Environment environment,
      IErrorLogger errorLog,
      RelativePath sourceFile,
      RelativePath sourceFileFromResult) throws IOException {
    
    String moduleName = null;
    if (isApplication(toplevelDecl, "ModuleDec")) {
      moduleName = prettyPrint(getApplicationSubterm(toplevelDecl, "ModuleDec", 0));
      jsSource.setModuleDecl(prettyPrint(toplevelDecl));
    } else if (isApplication(toplevelDecl, "SugarModuleDec")) {
      moduleName = prettyPrint(getApplicationSubterm(toplevelDecl, "SugarModuleDec", 0));
      jsSource.setModuleDecl(":-module(" + moduleName + ", []).");
    }
    
    relNamespaceName = FileCommands.dropFilename(sourceFile.getRelativePath());
    decName = getRelativeModulePath(moduleName);
    log.log("The SDF / Stratego package name is '" + relNamespaceName + "'.", Log.DETAIL);
    
    if (jsOutFile == null) 
      jsOutFile = environment.createBinPath(getRelativeNamespaceSep() + FileCommands.fileName(sourceFileFromResult) + "." + getGeneratedFileExtension());
  }
  

  @Override
  public LanguageLibFactory getFactoryForLanguage() {
    return JavaScriptLibFactory.getInstance();
  }

  @Override
  protected void compile(List<Path> sourceFiles, Path bin, List<Path> path,
      boolean generateFiles)
      throws IOException {

    if (generateFiles) {
      for (Path file : sourceFiles) {
        // XXX: do nothing here?
        System.err.println("javascript;     no compilation neccessary, file: " + file);
      }
    }

  }
  
  @Override
  public String getImportedModulePath(IStrategoTerm toplevelDecl) throws IOException {
    String modulePath = prettyPrint(toplevelDecl.getSubterm(0).getSubterm(0));
    
    return modulePath;    
  }
  
  private String getRelativeModulePath(String moduleName) {
    return moduleName.replace("/", Environment.sep);
  }
  
  @Override
  public void addImportModule(IStrategoTerm toplevelDecl, boolean checked) throws IOException {
    
    String importedModuleName = prettyPrint(toplevelDecl.getSubterm(0).getSubterm(0));
    JavaScriptModuleImport imp = jsSource.getImport(importedModuleName, toplevelDecl);
    
    if (checked)
      jsSource.addCheckedImport(imp);
    else
      jsSource.addImport(imp);  
  }

  @Override
  public String getSugarName(IStrategoTerm decl) throws IOException {
        return decName;
  }


  @Override
  public IStrategoTerm getSugarBody(IStrategoTerm decl) {
    IStrategoTerm sugarBody = getApplicationSubterm(decl, "SugarBody", 0);
    
    return sugarBody;

  }
  
  @Override
  public String getLanguageName() {
    return "JavaScript";
  }

  @Override
  public boolean isModuleResolvable(String relModulePath) {
      return false;
  }

  @Override
  public String getEditorName(IStrategoTerm decl) throws IOException {
    throw new UnsupportedOperationException("SugarProlog does currently not support editor libraries.");
  }

  @Override
  public IStrategoTerm getEditorServices(IStrategoTerm decl) {
    throw new UnsupportedOperationException("SugarProlog does currently not support editor libraries.");
  }


  
  
}