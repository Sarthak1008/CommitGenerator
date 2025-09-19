package com.AutoCommitter.CommitGenerator.Git;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Reads staged changes and produces unified diff text for each staged file.
 */
public class StagedDiffService {

    public static class StagedDiff {
        public final String filePath;
        public final String changeType;
        public final String patch;

        public StagedDiff(String filePath, String changeType, String patch) {
            this.filePath = filePath;
            this.changeType = changeType;
            this.patch = patch;
        }
    }

    public List<StagedDiff> readStagedDiffs(File repoRoot) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
                .findGitDir(repoRoot)
                .readEnvironment();
        try (Repository repository = builder.build();
             Git git = new Git(repository)) {

            Status status = git.status().call();
            Set<String> added = status.getAdded();
            Set<String> removed = status.getRemoved();

            List<StagedDiff> diffs = new ArrayList<>();

            try {
                List<DiffEntry> entries = git.diff()
                        .setCached(true) // staged
                        .call();

                for (DiffEntry entry : entries) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (DiffFormatter formatter = new DiffFormatter(out)) {
                        formatter.setRepository(repository);
                        formatter.format(entry);
                    }
                    String patch = out.toString();
                    diffs.add(new StagedDiff(entry.getNewPath(), entry.getChangeType().name(), patch));
                }
            } catch (NoHeadException e) {
                // Initial commit: no HEAD yet; rely on status added/removed lists below
            } catch (Exception e) {
                throw new IOException("Failed to compute staged diffs", e);
            }

            for (String a : added) {
                diffs.add(new StagedDiff(a, "ADD", ""));
            }
            for (String r : removed) {
                diffs.add(new StagedDiff(r, "DELETE", ""));
            }

            return diffs;
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException(e);
        }
    }
}


