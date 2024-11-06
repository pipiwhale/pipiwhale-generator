package com.poke.cli.example;

import picocli.CommandLine;

@CommandLine.Command
public class TestInteractive implements Runnable {
    @CommandLine.Option(names = "--interactive", interactive = true)
    String value;

    public void run() {
        if (value == null && System.console() != null) {
            // 主动提示用户输入
            value = System.console().readLine("Enter value for --interactive: ");
        }
        System.out.println("You provided value '" + value + "'");
    }

    public static void main(String[] args) {
//        args = new String[] { "--interactive", "123" };
        new CommandLine(new TestInteractive()).execute(args);
    }
}
