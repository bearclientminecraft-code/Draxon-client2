package dev.draxon.setting;
import org.lwjgl.glfw.GLFW;
public class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String n, int key) { super(n, key); }
    @Override public String serialize() { return String.valueOf(value); }
    @Override public void deserialize(String s) { value = Integer.parseInt(s); }
    public static String keyName(int k) {
        if (k < 0) return "None";
        switch (k) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R-Shift"; case GLFW.GLFW_KEY_LEFT_SHIFT: return "L-Shift";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R-Ctrl"; case GLFW.GLFW_KEY_LEFT_CONTROL: return "L-Ctrl";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "R-Alt"; case GLFW.GLFW_KEY_LEFT_ALT: return "L-Alt";
            case GLFW.GLFW_KEY_SPACE: return "Space"; case GLFW.GLFW_KEY_TAB: return "Tab"; case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_UP: return "Up"; case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left"; case GLFW.GLFW_KEY_RIGHT: return "Right";
            default:
        }
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F12) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(k, 0);
        return n != null ? n.toUpperCase() : "Key " + k;
    }
}
