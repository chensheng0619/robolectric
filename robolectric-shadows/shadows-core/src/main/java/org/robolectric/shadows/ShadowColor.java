package org.robolectric.shadows;

import android.graphics.Color;
import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import org.robolectric.internal.Shadow;
import org.robolectric.util.ReflectionHelpers;

@Implements(Color.class)
public class ShadowColor {

  @Implementation
  public static int parseColor(String colorString) {
    if (colorString.charAt(0) == '#' && (colorString.length() == 4 || colorString.length() == 5)) {
      StringBuilder buf = new StringBuilder();
      buf.append('#');
      for (int i = 1; i < colorString.length(); i++) {
        buf.append(colorString.charAt(i));
        buf.append(colorString.charAt(i));
      }
      colorString = buf.toString();
    }
    try {
      return (int) Shadow.directlyOn(Color.class, "parseColor",
          ReflectionHelpers.ClassParameter.from(String.class, colorString));
    } catch (Exception e) {
      throw new IllegalArgumentException("Can't parse value from color \"" + colorString + "\"", e);
    }
  }

  /**
   * This is implemented in native code in the Android SDK.
   *
   * <p>Since HSV == HSB then the implementation from {@link java.awt.Color} can be used,
   * with a small adjustment to the representation of the hue.</p>
   *
   * <p>{@link java.awt.Color} represents hue as 0..1 (where 1 == 100% == 360 degrees),
   * while {@link android.graphics.Color} represents hue as 0..360 degrees. The correct hue can be calculated
   * by multiplying with 360.</p>
   * @param red Red component
   * @param green Green component
   * @param blue Blue component
   * @param hsv Array to store HSV components
   */
  @Implementation
  public static void RGBToHSV(int red, int green, int blue, float hsv[]) {
    java.awt.Color.RGBtoHSB(red, green, blue, hsv);
    hsv[0] = hsv[0] * 360;
  }

  @Implementation
  public static int HSVToColor(int alpha, float hsv[]) {
    int rgb = java.awt.Color.HSBtoRGB(hsv[0] / 360, hsv[1], hsv[2]);
    return Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
  }
}
