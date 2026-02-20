package org.example.artyom.magicMechanism.data.enums;

import java.util.List;

public interface Screen extends ScreenCategory {
    List<MenuAction> getActions();
}
