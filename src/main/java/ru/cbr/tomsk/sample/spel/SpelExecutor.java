package ru.cbr.tomsk.sample.spel;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.DoubleStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

public class SpelExecutor {

    private static final Log log = LogFactory.getLog(SpelExecutor.class);

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Properties p = new Properties();
        p.load(new FileReader("values.properties"));
        StandardEvaluationContext context = new StandardEvaluationContext(p);
        context.setPropertyAccessors(Arrays.asList(new MapPropertyAccessor()));
        context.addMethodResolver(new AgrMethodResolver());
        ExpressionParser parser = new SpelExpressionParser();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line = readLine(br);
            while (line != null && !"EXIT".equalsIgnoreCase(line)) {
                try {
                    Expression exp = parser.parseExpression(line);
                    System.out.println("Result: " + exp.getValue(context));
                } catch (Exception ex) {
                    System.out.println("ERROR: " + ex.getMessage() + " (" + ex + ")");
                    log.error("processing error", ex);
                }
                line = readLine(br);
            }
        }
    }

    private static String readLine(BufferedReader br) throws IOException {
        System.out.print(">");
        return br.readLine();
    }

    private static class AgrMethodResolver implements MethodResolver {

        @Override
        public MethodExecutor resolve(EvaluationContext ec, Object target, String name, List<TypeDescriptor> list) throws AccessException {
            switch (name) {
                case "SUM":
                    return new SumMethodExecutor();
            }
            return null;
        }

    }

    private static class SumMethodExecutor implements MethodExecutor {

        @Override
        public TypedValue execute(EvaluationContext ec, Object name, Object... args) throws AccessException {
            double res = Arrays.stream(args)
                    .flatMapToDouble(SumMethodExecutor::toStream)
                    .sum();
            return new TypedValue(res);
        }

        private static DoubleStream toStream(Object v) {
            if (v instanceof Number) {
                return DoubleStream.of(((Number) v).doubleValue());
            }
            if (v instanceof String) {
                String s = (String) v;
                if (s.contains(",")) {
                    String[] sa = s.split(",");
                    return Arrays.stream(sa)
                            .mapToDouble(d -> Double.parseDouble(d));
                }
            }
            throw new RuntimeException("invalid arguments: " + v);
        }

    }

    private static class MapPropertyAccessor implements PropertyAccessor {

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            log.debug("getSpecificTargetClasses()...");
            return new Class<?>[]{Map.class};
        }

        @Override
        public boolean canRead(EvaluationContext ec, Object target, String name) throws AccessException {
            log.debug("canRead(...," + target + "," + name + ")...");
            return true;
        }

        @Override
        public TypedValue read(EvaluationContext ec, Object target, String name) throws AccessException {
            log.debug("read(...," + target + "," + name + ")...");
            Assert.state(target instanceof Map, "target must be of type Map");
            Object v = ((Map) target).get(name);
            if (v instanceof String) {
                String s = (String) v;
                try {
                    v = Long.parseLong(s);
                } catch (NumberFormatException ex1) {
                    log.debug("'" + v + "' is not digital value", ex1);
                    try {
                        v = Double.parseDouble((String) v);
                    } catch (NumberFormatException ex2) {
                        log.debug("'" + v + "' is not digital value", ex2);
                    }
                }
            }
            return new TypedValue(v);
        }

        @Override
        public boolean canWrite(EvaluationContext ec, Object target, String name) throws AccessException {
            log.debug("canWrite(...," + target + "," + name + ")...");
            return true;
        }

        @Override
        public void write(EvaluationContext ec, Object target, String name, Object value) throws AccessException {
            log.debug("write(...," + target + "," + name + "," + value + ")...");
            ((Map) target).put(name, value);
        }

    }
}
