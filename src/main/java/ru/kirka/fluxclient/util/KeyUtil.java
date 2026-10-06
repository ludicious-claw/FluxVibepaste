package ru.kirka.fluxclient.util;

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public class KeyUtil {
   private static final Map<Integer, String> KEY_NAMES = new HashMap<>();

   public static String getKeyName(int code) {
      if (code == -1) {
         return "None";
      } else {
         String name = KEY_NAMES.get(code);
         if (name != null) {
            return name;
         } else {
            String glfw = GLFW.glfwGetKeyName(code, 0);
            return glfw != null && !glfw.isBlank() ? glfw.toUpperCase() : "Key " + code;
         }
      }
   }

   public static String b(int code) {
      return getKeyName(code);
   }

   static {
      KEY_NAMES.put(32, "Space");
      KEY_NAMES.put(39, "'");
      KEY_NAMES.put(44, ",");
      KEY_NAMES.put(45, "-");
      KEY_NAMES.put(46, ".");
      KEY_NAMES.put(47, "/");
      KEY_NAMES.put(59, ";");
      KEY_NAMES.put(61, "=");
      KEY_NAMES.put(91, "[");
      KEY_NAMES.put(92, "\\");
      KEY_NAMES.put(93, "]");
      KEY_NAMES.put(96, "`");
      KEY_NAMES.put(256, "Esc");
      KEY_NAMES.put(257, "Enter");
      KEY_NAMES.put(258, "Tab");
      KEY_NAMES.put(259, "Backspace");
      KEY_NAMES.put(260, "Insert");
      KEY_NAMES.put(261, "Delete");
      KEY_NAMES.put(262, "Right");
      KEY_NAMES.put(263, "Left");
      KEY_NAMES.put(264, "Down");
      KEY_NAMES.put(265, "Up");
      KEY_NAMES.put(266, "Page Up");
      KEY_NAMES.put(267, "Page Down");
      KEY_NAMES.put(268, "Home");
      KEY_NAMES.put(269, "End");
      KEY_NAMES.put(280, "Caps");
      KEY_NAMES.put(281, "Scroll");
      KEY_NAMES.put(282, "Num Lock");
      KEY_NAMES.put(283, "PrtSc");
      KEY_NAMES.put(284, "Pause");
      KEY_NAMES.put(340, "LShift");
      KEY_NAMES.put(341, "LCtrl");
      KEY_NAMES.put(342, "LAlt");
      KEY_NAMES.put(344, "RShift");
      KEY_NAMES.put(345, "RCtrl");
      KEY_NAMES.put(346, "RAlt");
      KEY_NAMES.put(343, "Win");
      KEY_NAMES.put(347, "RWin");
      KEY_NAMES.put(348, "Menu");
      KEY_NAMES.put(320, "Num 0");
      KEY_NAMES.put(321, "Num 1");
      KEY_NAMES.put(322, "Num 2");
      KEY_NAMES.put(323, "Num 3");
      KEY_NAMES.put(324, "Num 4");
      KEY_NAMES.put(325, "Num 5");
      KEY_NAMES.put(326, "Num 6");
      KEY_NAMES.put(327, "Num 7");
      KEY_NAMES.put(328, "Num 8");
      KEY_NAMES.put(329, "Num 9");
      KEY_NAMES.put(330, "Num .");
      KEY_NAMES.put(331, "Num /");
      KEY_NAMES.put(332, "Num *");
      KEY_NAMES.put(333, "Num -");
      KEY_NAMES.put(334, "Num +");
      KEY_NAMES.put(335, "Num Enter");
      KEY_NAMES.put(336, "Num =");

      for (int i = 0; i <= 9; i++) {
         KEY_NAMES.put(48 + i, String.valueOf(i));
      }

      for (int i = 0; i < 26; i++) {
         KEY_NAMES.put(65 + i, String.valueOf((char)(65 + i)));
      }

      for (int i = 1; i <= 25; i++) {
         KEY_NAMES.put(290 + (i - 1), "F" + i);
      }

      KEY_NAMES.put(-100, "M1");
      KEY_NAMES.put(-99, "M2");
      KEY_NAMES.put(-98, "M3");
      KEY_NAMES.put(-97, "M4");
      KEY_NAMES.put(-96, "M5");
   }
}
