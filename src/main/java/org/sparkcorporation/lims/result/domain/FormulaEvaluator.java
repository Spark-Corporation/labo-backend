package org.sparkcorporation.lims.result.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Pure, dependency-free arithmetic evaluator for catalog {@code ResultType.FORMULA} (CDC §Config.8
 * "Formule"). Recursive descent over: {@code expr := term (('+'|'-') term)*}, {@code term := factor
 * (('*'|'/') factor)*}, {@code factor := NUMBER | IDENT | '(' expr ')' | '-' factor}. Identifiers
 * are analysis codes resolved against the supplied value map.
 *
 * <p>Total by design: any failure — unknown/blank identifier, divide-by-zero, or malformed input —
 * yields {@link Optional#empty()} and never throws, so one bad formula cannot block result entry
 * for the rest of a dossier.
 */
public class FormulaEvaluator {

    private static final MathContext MC = MathContext.DECIMAL64;

    /** Evaluates {@code expression} against {@code values}; empty on any failure (never throws). */
    public Optional<BigDecimal> evaluate(String expression, Map<String, BigDecimal> values) {
        try {
            var parser = new Parser(expression, values);
            BigDecimal result = parser.parseExpression();
            parser.expectEnd();
            return Optional.of(result);
        } catch (EvalException | ArithmeticException e) {
            return Optional.empty();
        }
    }

    /** Identifiers (analysis codes) referenced by the expression; empty on malformed input. */
    public Set<String> referencedCodes(String expression) {
        var codes = new LinkedHashSet<String>();
        try {
            new Lexer(expression).forEachIdentifier(codes::add);
        } catch (EvalException e) {
            return Set.of();
        }
        return codes;
    }

    private static final class EvalException extends RuntimeException {
        EvalException(String m) {
            super(m);
        }
    }

    /** Hand tokenizer + recursive-descent parser. */
    private static final class Parser {
        private final String src;
        private final Map<String, BigDecimal> values;
        private int pos;

        Parser(String src, Map<String, BigDecimal> values) {
            this.src = src;
            this.values = values;
        }

        BigDecimal parseExpression() {
            BigDecimal v = parseTerm();
            while (true) {
                char op = peekOp();
                if (op == '+') {
                    next();
                    v = v.add(parseTerm(), MC);
                } else if (op == '-') {
                    next();
                    v = v.subtract(parseTerm(), MC);
                } else {
                    return v;
                }
            }
        }

        private BigDecimal parseTerm() {
            BigDecimal v = parseFactor();
            while (true) {
                char op = peekOp();
                if (op == '*') {
                    next();
                    v = v.multiply(parseFactor(), MC);
                } else if (op == '/') {
                    next();
                    BigDecimal d = parseFactor();
                    if (d.signum() == 0) {
                        throw new ArithmeticException("divide by zero");
                    }
                    v = v.divide(d, MC);
                } else {
                    return v;
                }
            }
        }

        private BigDecimal parseFactor() {
            skipWs();
            char c = peek();
            if (c == '-') {
                next();
                return parseFactor().negate();
            }
            if (c == '(') {
                next();
                BigDecimal v = parseExpression();
                skipWs();
                expect(')');
                return v;
            }
            if (Character.isDigit(c) || c == '.') {
                return number();
            }
            if (Character.isLetter(c) || c == '_') {
                String ident = identifier();
                BigDecimal v = values.get(ident);
                if (v == null) {
                    throw new EvalException("unresolved code: " + ident);
                }
                return v;
            }
            throw new EvalException("unexpected char at " + pos);
        }

        private BigDecimal number() {
            int start = pos;
            while (pos < src.length()
                    && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
                pos++;
            }
            try {
                return new BigDecimal(src.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new EvalException("bad number");
            }
        }

        private String identifier() {
            int start = pos;
            while (pos < src.length()
                    && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                pos++;
            }
            return src.substring(start, pos);
        }

        char peekOp() {
            skipWs();
            return peek();
        }

        private char peek() {
            return pos < src.length() ? src.charAt(pos) : '\0';
        }

        private void next() {
            pos++;
        }

        private void expect(char c) {
            if (peek() != c) {
                throw new EvalException("expected " + c);
            }
            pos++;
        }

        void expectEnd() {
            skipWs();
            if (pos != src.length()) {
                throw new EvalException("trailing input");
            }
        }

        private void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }
    }

    /** Minimal lexer used only by {@link #referencedCodes}. */
    private static final class Lexer {
        private final String src;

        Lexer(String src) {
            this.src = src;
        }

        void forEachIdentifier(Consumer<String> sink) {
            int i = 0;
            while (i < src.length()) {
                char c = src.charAt(i);
                if (Character.isLetter(c) || c == '_') {
                    int start = i;
                    while (i < src.length()
                            && (Character.isLetterOrDigit(src.charAt(i)) || src.charAt(i) == '_')) {
                        i++;
                    }
                    sink.accept(src.substring(start, i));
                } else if (Character.isDigit(c)
                        || c == '.'
                        || Character.isWhitespace(c)
                        || "+-*/()".indexOf(c) >= 0) {
                    i++;
                } else {
                    throw new EvalException("bad char");
                }
            }
        }
    }
}
