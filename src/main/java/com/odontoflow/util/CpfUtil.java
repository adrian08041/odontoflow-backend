package com.odontoflow.util;

/** Utilitário de CPF: normalização, validação de dígitos verificadores e formatação canônica. */
public final class CpfUtil {

    private CpfUtil() {}

    /** Remove tudo que não for dígito. Nunca retorna null. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\D", "");
    }

    /** Formata 11 dígitos no padrão canônico do app: 000.000.000-00. */
    public static String format(String digits) {
        if (digits == null || digits.length() != 11) {
            throw new IllegalArgumentException("CPF deve ter 11 dígitos para formatação");
        }
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "."
                + digits.substring(6, 9) + "-" + digits.substring(9, 11);
    }

    /** Valida CPF: 11 dígitos, não-sequência trivial (000..., 111...) e dígitos verificadores corretos. */
    public static boolean isValid(String raw) {
        String cpf = normalize(raw);
        if (cpf.length() != 11) return false;
        if (cpf.chars().distinct().count() == 1) return false;
        return checkDigit(cpf, 9, 10) == (cpf.charAt(9) - '0')
                && checkDigit(cpf, 10, 11) == (cpf.charAt(10) - '0');
    }

    private static int checkDigit(String cpf, int length, int startWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (startWeight - i);
        }
        int mod = sum % 11;
        return (mod < 2) ? 0 : 11 - mod;
    }
}
