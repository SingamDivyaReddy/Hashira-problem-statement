import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.json.JSONObject;

class BigRational {
    BigInteger num; // numerator
    BigInteger den; // denominator

    public BigRational(BigInteger num, BigInteger den) {
        if (den.equals(BigInteger.ZERO)) throw new ArithmeticException("Denominator 0");
        // Normalize denominator
        if (den.signum() < 0) {
            num = num.negate();
            den = den.negate();
        }
        BigInteger g = num.gcd(den);
        this.num = num.divide(g);
        this.den = den.divide(g);
    }

    public BigRational add(BigRational other) {
        BigInteger n = this.num.multiply(other.den).add(other.num.multiply(this.den));
        BigInteger d = this.den.multiply(other.den);
        return new BigRational(n, d);
    }

    public BigRational multiply(BigRational other) {
        return new BigRational(this.num.multiply(other.num), this.den.multiply(other.den));
    }

    public BigRational divide(BigRational other) {
        return new BigRational(this.num.multiply(other.den), this.den.multiply(other.num));
    }

    public String toString() {
        if (den.equals(BigInteger.ONE)) return num.toString();
        return num + "/" + den;
    }

    public BigInteger toBigInteger() {
        if (!den.equals(BigInteger.ONE)) {
            throw new ArithmeticException("Result is not an integer: " + this);
        }
        return num;
    }
}

public class PolynomialConstantFinder {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java PolynomialConstantFinder <json-file>");
            return;
        }

        // Read JSON file given by user
        String filename = args[0];
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        JSONObject json = new JSONObject(content);

        JSONObject keys = json.getJSONObject("keys");
        int n = keys.getInt("n");
        int k = keys.getInt("k");

        // Parse x,y pairs
        List<Integer> xs = new ArrayList<>();
        List<BigInteger> ys = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            if (!json.has(String.valueOf(i))) continue;
            JSONObject point = json.getJSONObject(String.valueOf(i));
            int base = Integer.parseInt(point.getString("base"));
            String value = point.getString("value");
            BigInteger y = new BigInteger(value, base);
            xs.add(i);
            ys.add(y);
        }

        // Use first k points
        xs = xs.subList(0, k);
        ys = ys.subList(0, k);

        // Lagrange interpolation at x=0
        BigRational result = new BigRational(BigInteger.ZERO, BigInteger.ONE);

        for (int i = 0; i < k; i++) {
            BigRational term = new BigRational(ys.get(i), BigInteger.ONE);
            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger num = BigInteger.valueOf(-xs.get(j));
                BigInteger den = BigInteger.valueOf(xs.get(i) - xs.get(j));
                term = term.multiply(new BigRational(num, den));
            }
            result = result.add(term);
        }

        // Final output
        System.out.println("Constant term (c) = " + result);
        try {
            System.out.println("As integer: " + result.toBigInteger());
        } catch (Exception e) {
            System.out.println("Not an integer, rational form shown.");
        }
    }
}
