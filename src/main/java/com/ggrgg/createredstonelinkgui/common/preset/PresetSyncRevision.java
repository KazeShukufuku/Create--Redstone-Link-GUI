package com.ggrgg.createredstonelinkgui.common.preset;

public class PresetSyncRevision {

    private static int localRevision;
    private static int confirmedRevision;

    public static int nextLocalRevision() {
        return ++localRevision;
    }

    public static boolean shouldApplyServerSync(int revision) {
        if (revision == 0) {
            return localRevision == 0;
        }
        if (revision < localRevision || revision < confirmedRevision) {
            return false;
        }
        confirmedRevision = revision;
        return true;
    }
}
