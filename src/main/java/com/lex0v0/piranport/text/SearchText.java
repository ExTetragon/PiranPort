package com.lex0v0.piranport.text;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

// 两类制造查询共用全半角、大小写与标点处理。
public final class SearchText {
    private SearchText() {}
    private static String normalize(String text) {
        var result = new StringBuilder();
        Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).codePoints()
                .filter(Character::isLetterOrDigit).forEach(result::appendCodePoint);
        return result.toString();
    }
    public static boolean matches(String text, List<String> names) {
        String query = normalize(text);
        return query.isEmpty() || names.stream().anyMatch(name -> normalize(name).contains(query));
    }
}
