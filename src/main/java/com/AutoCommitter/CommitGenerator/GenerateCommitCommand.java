package com.AutoCommitter.CommitGenerator;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.List;
import com.AutoCommitter.CommitGenerator.Git.StagedDiffService;


@Component
public class GenerateCommitCommand implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0 || !"generate-commit".equals(args[0])) {
            return; // Not our command; allow normal Spring Boot app startup
        }

        boolean doCommit = false;
        for (String arg : args) {
            if ("--commit".equals(arg) || "-c".equals(arg)) {
                doCommit = true;
            }
        }

        File repoRoot = new File(".");
        StagedDiffService diffService = new StagedDiffService();
        List<StagedDiffService.StagedDiff> diffs = diffService.readStagedDiffs(repoRoot);

        SimpleCommitMessageGenerator generator = new SimpleCommitMessageGenerator();
        String message = generator.generate(diffs);

        System.out.println(message);

        if (doCommit) {
            try (org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.open(repoRoot)) {
                git.commit().setMessage(message).call();
                System.out.println("Committed with generated message.");
            }
        }

        // Exit after running command to avoid starting web server
        System.exit(0);
    }
}


