package folk.sisby.surveyor.util;

import net.minecraft.text.Text;

public class TextUtil {
    public static Text stripInteraction(Text text) {
        return text.copy().styled(s -> s.withClickEvent(null).withHoverEvent(null).withInsertion(null));
    }
}
