package io.cloudslang.runtime.api.java;

import java.util.List;

public interface JavaRuntimeService {

    /**
     * @param dependencies - list of resources with maven GAV notation ‘groupId:artifactId:version’ which can be used to resolve resources with Maven Repository Support
     */

    Object execute (String className, String methodName, List<Object> args, List<String> dependencies);
}
