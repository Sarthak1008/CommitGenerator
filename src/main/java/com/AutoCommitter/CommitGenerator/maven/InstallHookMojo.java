package com.AutoCommitter.CommitGenerator.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Goal to install the prepare-commit-msg Git hook
 */
@Mojo(name = "install-hook", defaultPhase = LifecyclePhase.INITIALIZE)
public class InstallHookMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            File baseDir = project.getBasedir();
            File gitDir = new File(baseDir, ".git");
            
            if (!gitDir.exists()) {
                getLog().warn("No .git directory found. Skipping Git hook installation.");
                return;
            }
            
            File hooksDir = new File(gitDir, "hooks");
            if (!hooksDir.exists()) {
                hooksDir.mkdirs();
            }
            
            // Determine OS and create appropriate hook file
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            
            if (isWindows) {
                installWindowsHook(hooksDir);
            } else {
                installUnixHook(hooksDir);
            }
            
            getLog().info("CommitGenerator Git hook installed successfully.");
            
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to install Git hook", e);
        }
    }
    
    private void installWindowsHook(File hooksDir) throws IOException {
        File hookFile = new File(hooksDir, "prepare-commit-msg.ps1");
        String hookContent = 
            "param(\n" +
            "    [string]$CommitMsgFile\n" +
            ")\n\n" +
            "# Skip if not a commit message (e.g., --amend or merge)\n" +
            "if ($CommitMsgFile -match \"MERGE_MSG$\" -or $CommitMsgFile -match \"COMMIT_EDITMSG$\" -and (Get-Content $CommitMsgFile | Where-Object { $_ -notmatch \"^#\" } | Measure-Object).Count -gt 0) {\n" +
            "    exit 0\n" +
            "}\n\n" +
            "# Get the commit message from CommitGenerator\n" +
            "$jarPath = \"" + getJarPath() + "\"\n" +
            "$commitMsg = java -jar $jarPath\n\n" +
            "# Write the commit message to the file\n" +
            "Set-Content -Path $CommitMsgFile -Value $commitMsg\n";
        
        Files.write(hookFile.toPath(), hookContent.getBytes());
        getLog().info("Windows PowerShell hook installed at: " + hookFile.getAbsolutePath());
    }
    
    private void installUnixHook(File hooksDir) throws IOException {
        File hookFile = new File(hooksDir, "prepare-commit-msg");
        String hookContent = 
            "#!/bin/sh\n\n" +
            "# Skip if not a commit message (e.g., --amend or merge)\n" +
            "case \"$2,$3\" in \n" +
            "  merge,*|message,*) exit 0 ;;\n" +
            "esac\n\n" +
            "# Get the commit message from CommitGenerator\n" +
            "jarPath=\"" + getJarPath() + "\"\n" +
            "commitMsg=$(java -jar \"$jarPath\")\n\n" +
            "# Write the commit message to the file\n" +
            "echo \"$commitMsg\" > \"$1\"\n";
        
        Files.write(hookFile.toPath(), hookContent.getBytes());
        hookFile.setExecutable(true);
        getLog().info("Unix/Linux hook installed at: " + hookFile.getAbsolutePath());
    }
    
    private String getJarPath() {
        // Return the path to the CommitGenerator JAR
        return project.getBuild().getDirectory() + File.separator + 
               project.getBuild().getFinalName() + ".jar";
    }
}