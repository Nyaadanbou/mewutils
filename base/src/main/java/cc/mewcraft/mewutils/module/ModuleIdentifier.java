package cc.mewcraft.mewutils.module;

import com.google.common.base.CaseFormat;

public interface ModuleIdentifier {

    /**
     * Returns the long id of this module, which is upper camel case.
     * <p>
     * Example: `BetterBeehive`.
     *
     * @return the long id of this module
     */
    default String getLongId() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, getId());
    }

    /**
     * Returns the id of this module, which is the direct package name of the runtime class.
     * <p>
     * Example: `better_beehive`.
     *
     * @return the direct package name of the runtime class
     */
    default String getId() {
        String packageName = getClass().getPackage().getName();
        String directParent;
        if (packageName.contains(".")) {
            directParent = packageName.substring(1 + packageName.lastIndexOf("."));
        } else {
            directParent = packageName;
        }
        return directParent;
    }

}
