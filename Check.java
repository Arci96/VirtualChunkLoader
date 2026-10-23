package dev.virtualchunkloader.farm;

/** Eine einzelne tatsächlich geprüfte Bedingung. */
public record Check(Status status, String label, String detail) {

    public enum Status { OK, FAIL, WARN, INFO, UNKNOWN }

    public static Check ok(String label, String detail) { return new Check(Status.OK, label, detail); }
    public static Check fail(String label, String detail) { return new Check(Status.FAIL, label, detail); }
    public static Check warn(String label, String detail) { return new Check(Status.WARN, label, detail); }
    public static Check info(String label, String detail) { return new Check(Status.INFO, label, detail); }
    public static Check unknown(String label, String detail) { return new Check(Status.UNKNOWN, label, detail); }
}
