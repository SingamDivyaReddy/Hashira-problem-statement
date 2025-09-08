import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.json.JSONObject;

class Fraction {
    BigInteger num, den;

    public Fraction(BigInteger num, BigInteger den) {
        if (den.equals(BigInteger.ZERO)) throw new ArithmeticException("Denominator 0");
        if (den.signum() < 0) { num = num.negate(); den = den.negate(); }
        BigInteger g = num.gcd(den);
        this.num = num.divide(g);
        this.den = den.divide(g);
    }

    public static Fraction zero() { return new Fraction(BigInteger.ZERO, BigInteger.ONE); }
    public static Fraction fromBigInt(BigInteger b) { return new Fraction(b, BigInteger.ONE); }

    public Fraction add(Fraction other) { 
        return new Fraction(this.num.multiply(other.den).add(other.num.multiply(this.den)),
                            this.den.multiply(other.den));
    }
    public Fraction sub(Fraction other) {
        return new Fraction(this.num.multiply(other.den).subtract(other.num.multiply(this.den)),
                            this.den.multiply(other.den));
    }
    public Fraction mul(Fraction other) { 
        return new Fraction(this.num.multiply(other.num), this.den.multiply(other.den)); 
    }
    public Fraction div(Fraction other) { 
        if (other.num.equals(BigInteger.ZERO)) throw new ArithmeticException("Divide by zero");
        return new Fraction(this.num.multiply(other.den), this.den.multiply(other.num));
    }
    public Fraction neg() { return new Fraction(this.num.negate(), this.den); }

    public String toString() {
        return den.equals(BigInteger.ONE) ? num.toString() : num + "/" + den;
    }

    public BigInteger toIntegerIfExact() {
        if (den.equals(BigInteger.ONE)) return num;
        if (num.mod(den).equals(BigInteger.ZERO)) return num.divide(den);
        return null;
    }
}

class Point {
    BigInteger x;
    BigInteger y;
    public Point(BigInteger x, BigInteger y) { this.x = x; this.y = y; }
}

public class PolynomialSecret {
    
    // Parse BigInteger from string in given base
    public static BigInteger parseBigIntFromBase(String str, int base) {
        return new BigInteger(str.replace("_",""), base);
    }

    // Lagrange interpolation at x=0
    public static Fraction lagrangeAtZero(List<Point> pointsSubset) {
        Fraction sum = Fraction.zero();
        int k = pointsSubset.size();
        for (int i = 0; i < k; i++) {
            Fraction yi = Fraction.fromBigInt(pointsSubset.get(i).y);
            Fraction li = Fraction.fromBigInt(BigInteger.ONE);
            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xi = pointsSubset.get(i).x;
                BigInteger xj = pointsSubset.get(j).x;
                Fraction numer = new Fraction(xj.negate(), BigInteger.ONE);
                Fraction denom = new Fraction(xi.subtract(xj), BigInteger.ONE);
                li = li.mul(numer.div(denom));
            }
            sum = sum.add(yi.mul(li));
        }
        return sum;
    }

    // Generate all combinations of indices
    public static List<List<Integer>> combinations(int n, int k) {
        List<List<Integer>> out = new ArrayList<>();
        backtrack(out, new ArrayList<>(), 0, n, k);
        return out;
    }

    private static void backtrack(List<List<Integer>> out, List<Integer> chosen, int start, int n, int k) {
        if (chosen.size() == k) { out.add(new ArrayList<>(chosen)); return; }
        for (int i = start; i <= n - (k - chosen.size()); i++) {
            chosen.add(i);
            backtrack(out, chosen, i + 1, n, k);
            chosen.remove(chosen.size() - 1);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { System.out.println("Usage: java PolynomialSecret <json-file>"); return; }

        String filename = args[0];
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        JSONObject json = new JSONObject(content);

        JSONObject keys = json.getJSONObject("keys");
        int n = keys.getInt("n");
        int k = keys.getInt("k");

        List<Point> points = new ArrayList<>();
        for (String key : json.keySet()) {
            if (key.equals("keys")) continue;
            if (!key.matches("\\d+")) continue;
            int xNum = Integer.parseInt(key);
            JSONObject p = json.getJSONObject(key);
            int base = Integer.parseInt(p.getString("base"));
            String valueStr = p.getString("value");
            BigInteger y = parseBigIntFromBase(valueStr, base);
            points.add(new Point(BigInteger.valueOf(xNum), y));
        }

        points.sort(Comparator.comparing(p -> p.x));

        if (points.size() < k) throw new Exception("Not enough points to determine secret");

        // All combinations of size k
        List<List<Integer>> comb = combinations(points.size(), k);

        Map<String, List<Point>> subsetThatGave = new HashMap<>();
        Map<String, Integer> tally = new HashMap<>();

        for (List<Integer> c : comb) {
            List<Point> subsetPoints = new ArrayList<>();
            for (int i : c) subsetPoints.add(points.get(i));
            Fraction secretFrac = lagrangeAtZero(subsetPoints);
            String key = secretFrac.num.toString() + "/" + secretFrac.den.toString();
            tally.put(key, tally.getOrDefault(key, 0) + 1);
            subsetThatGave.putIfAbsent(key, subsetPoints);
        }

        String bestKey = null;
        int bestCount = -1;
        for (Map.Entry<String, Integer> e : tally.entrySet()) {
            if (e.getValue() > bestCount) { bestCount = e.getValue(); bestKey = e.getKey(); }
        }

        if (bestKey == null) throw new Exception("Could not determine secret");

        String[] parts = bestKey.split("/");
        Fraction secretFrac = new Fraction(new BigInteger(parts[0]), new BigInteger(parts[1]));
        BigInteger intValue = secretFrac.toIntegerIfExact();

        if (intValue != null) {
            System.out.println("Secret: " + intValue);
        } else {
            System.out.println("Secret (fraction): " + secretFrac.toString());
        }
    }
}
