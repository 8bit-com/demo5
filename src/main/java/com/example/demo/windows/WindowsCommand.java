package com.example.demo.windows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

// Запускает Windows команды и печатает их результат.
public class WindowsCommand {

    // Запускает команду и возвращает её вывод.
    public String run(List<String> command) {
        try {
            // Создаём Windows процесс из списка аргументов.
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            // Читаем stdout и stderr процесса.
            String output = readOutput(process);

            // Ждём завершения команды.
            int exitCode = process.waitFor();

            // Печатаем саму команду.
            System.out.println("COMMAND: " + String.join(" ", command));

            // Печатаем код завершения.
            System.out.println("EXIT: " + exitCode);

            // Печатаем вывод команды, если он есть.
            if (!output.isBlank()) {
                System.out.println(output);
            }

            // Если команда завершилась ошибкой, кидаем исключение.
            if (exitCode != 0) {
                throw new IllegalStateException("Windows command failed: " + String.join(" ", command));
            }

            // Возвращаем вывод команды.
            return output;
        } catch (Exception e) {
            // Заворачиваем любую ошибку в RuntimeException.
            throw new RuntimeException(e);
        }
    }

    // Запускает команду и игнорирует ошибку.
    public void runIgnoreError(List<String> command) {
        try {
            // Пробуем выполнить команду обычным способом.
            run(command);
        } catch (Exception e) {
            // Печатаем, что ошибка намеренно проигнорирована.
            System.out.println("COMMAND IGNORED ERROR: " + String.join(" ", command));

            // Печатаем текст ошибки.
            System.out.println(e.getMessage());
        }
    }

    // Читает вывод Windows процесса.
    private String readOutput(Process process) throws Exception {
        // Открываем reader поверх stdout процесса.
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset())
        )) {
            // Склеиваем все строки вывода в одну строку.
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
