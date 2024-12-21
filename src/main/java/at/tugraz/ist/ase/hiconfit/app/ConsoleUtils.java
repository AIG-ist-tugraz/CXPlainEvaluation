/*
 * Causality-based Explanation for Feature Model Configuration
 *
 * Copyright (c) 2024
 *
 * @author: Viet-Man Le (vietman.le@ist.tugraz.at)
 */

package at.tugraz.ist.ase.hiconfit.app;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;

import static java.lang.System.out;

@Slf4j
public class ConsoleUtils {

    public static void printMessage(String message, BufferedWriter writer) {
        out.println(message);
        if (writer != null) {
            try {
                writer.write(message); writer.newLine();
                writer.flush();
            } catch (IOException e) {
                log.error("Error while writing message to file", e);
            }
        }
    }

}
