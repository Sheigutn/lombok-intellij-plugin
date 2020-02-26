package de.plushnikov.extensionmethod;

import lombok.experimental.ExtensionMethod;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

@ExtensionMethod({java.util.Arrays.class, ExtensionMethodPlain.Extensions.class})
public class ExtensionMethodPlain {
  public String test() {
    int[] intArray = {5, 3, 8, 2};
    float[] intArray2 = {5, 3, 8, 2};
    Integer[] integerArray = new Integer[0];
    val test1 = "".or("Test");

    String iAmNull = null;
    List<String> test = new ArrayList<>();

    return iAmNull.or("hELlO, WORlD!".toTitleCase());
  }

  static class Extensions {

    public static <T> T or(T obj, T ifNull) {
      return obj != null ? obj : ifNull;
    }

    public static String toTitleCase(String in) {
      if (in.isEmpty()) {
        return in;
      }
      return "" + Character.toTitleCase(in.charAt(0)) + in.substring(1).toLowerCase();
    }

    public static <T> List<T> test(List<T> testParameter) {
      return testParameter;
    }
  }
}

class Test2 {
  public static void main(String[] args) {
    String test = "".or("test");
  }
}
