package com.takehome.schedule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class TestSupport {

    private TestSupport() {
    }

    static Path writeFile(Path directory, String filename, String contents) throws IOException {
        Path file = directory.resolve(filename);
        Files.writeString(file, contents);
        return file;
    }

    static CommandResult runCommand(String... args) {
        ByteArrayOutputStream stdoutBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBytes = new ByteArrayOutputStream();

        int exitCode = Main.run(
                args,
                new PrintStream(stdoutBytes, true, StandardCharsets.UTF_8),
                new PrintStream(stderrBytes, true, StandardCharsets.UTF_8)
        );

        return new CommandResult(
                exitCode,
                stdoutBytes.toString(StandardCharsets.UTF_8),
                stderrBytes.toString(StandardCharsets.UTF_8)
        );
    }

    record CommandResult(int exitCode, String stdout, String stderr) {

    }
}
