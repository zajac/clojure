package clojure.lang;

import java.lang.invoke.*;

public class VarBootstrap {

//    public static CallSite bootstrapInvoke(MethodHandles.Lookup caller,
//                                           String methodName,
//                                           MethodType callsiteType,
//                                           String varNS, String varName) {
//        try {
//            Var var = RT.var(varNS, varName);
//            IFn fn = var.fn();
//            MethodHandle invoke = caller.findVirtual(IFn.class, "invoke", callsiteType);
//            return new ConstantCallSite(
//                    var.switchPoint.guardWithTest(
//                            invoke.bindTo(fn),
//                            invoke.bindTo(var)));
//        } catch (IllegalAccessException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static CallSite bootstrapInvokePrim(MethodHandles.Lookup caller,
//                                               String methodName,
//                                               MethodType callsiteType,
//                                               String varNS, String varName) {
//        try {
//            Var var = RT.var(varNS, varName);
//            IFn fn = var.fn();
//            MethodHandle invokePrim = caller.findVirtual(fn.getClass(), "invokePrim", callsiteType).bindTo(fn);
//            MethodHandle fallback = getIFnInvoke(caller, callsiteType).bindTo(var).asType(callsiteType);
//            return new ConstantCallSite(
//                    var.switchPoint.guardWithTest(
//                            invokePrim,
//                            fallback));
//
//        } catch (IllegalAccessException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static CallSite bootstrapInvokeStatic(MethodHandles.Lookup caller,
                                                 String methodName,
                                                 MethodType callsiteType,
                                                 String varNS, String varName) {
        try {
            Var var = RT.var(varNS, varName);
            IFn staticFn = var.fn();
            MethodHandle invokeStatic = caller.findStatic(staticFn.getClass(), "invokeStatic", callsiteType);
            MethodHandle fallback = getIFnInvoke(caller, callsiteType).bindTo(var).asType(callsiteType);
            return new ConstantCallSite(var.switchPoint.guardWithTest(invokeStatic, fallback));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite bootstrapDeref(MethodHandles.Lookup caller,
                                          String methodName,
                                          MethodType callsiteType,
                                          String varNS, String varName) {
        try {
            Var var = RT.var(varNS, varName);
            MethodHandle fallback = caller.findVirtual(Var.class, "get", MethodType.methodType(Object.class)).bindTo(var);
            if (var.isDynamic()) {
                return new ConstantCallSite(fallback);
            } else {
                Object value = var.getRawRoot();
                if (var.switchPoint != null) {
                    return new ConstantCallSite(
                            var.switchPoint.guardWithTest(MethodHandles.constant(Object.class, value), fallback));
                } else {
                    return new ConstantCallSite(fallback);
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite bootstrapAcquire(MethodHandles.Lookup caller,
                                           String methodName,
                                           MethodType callsiteType,
                                           String varNS, String varName) {
        Var var = RT.var(varNS, varName);
        return new ConstantCallSite(MethodHandles.constant(Var.class, var));
    }

    private static MethodHandle getIFnInvoke(MethodHandles.Lookup lookup, MethodType callsiteType) {
        try {
            MethodType boxedMethodType = callsiteType.changeReturnType(Object.class);
            for (int i = 0; i < callsiteType.parameterCount(); i++) {
                boxedMethodType = boxedMethodType.changeParameterType(i, Object.class);
            }
            return lookup.findVirtual(IFn.class, "invoke", boxedMethodType);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
