package org.suancai.toolbag.lib1236jdbc.exception;

/**
 * @author: SuricSun
 * @date: 2022/2/24
 */
public class Convert_exception extends Exception {

    public Exception inner_exception;

    public Convert_exception(Exception inner_exception) {

        super("Cant convert to list: " + inner_exception.getMessage() + " (Original exception name: " + inner_exception.getClass().getCanonicalName() + ")");
        this.inner_exception = inner_exception;
        this.setStackTrace(this.inner_exception.getStackTrace());
    }
}
