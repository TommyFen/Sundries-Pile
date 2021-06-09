package me.tommy.autoviewbind.launcher;

import me.tommy.autoviewbind.IAutoBind;

public class AutoBindView {

    private AutoBindView(){}

    public static AutoBindView getInstance() {
        return Holder.INSTANCE;
    }

    public void inject(Object target) {
        String className = target.getClass().getCanonicalName();
        String helperName = className + "$$AutoBind";
        try {
            IAutoBind helper = (IAutoBind) Class.forName(helperName).getConstructor().newInstance();
            helper.inject(target);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class Holder {
        private static final AutoBindView INSTANCE = new AutoBindView();
    }
}
