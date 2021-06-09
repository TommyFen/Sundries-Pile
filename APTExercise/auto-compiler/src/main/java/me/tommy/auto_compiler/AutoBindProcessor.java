package me.tommy.auto_compiler;

import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import me.tommy.autoviewbind.AutoBind;


/***
 * 参考作者：LiMubai
 * 链接：https://juejin.cn/post/6844903696900292621
 */
public class AutoBindProcessor extends AbstractProcessor {

    Filer filer;
    Types typeUtils;
    Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //合成java文件所需工具类
        filer = processingEnvironment.getFiler();
        //Class类型处理工具
        typeUtils = processingEnvironment.getTypeUtils();
        //注解所在的代码元素，分为类、字段、方法三种元素，一种新的概念
        elementUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {

        Set<String> supportAnnotation = new LinkedHashSet<>();
        supportAnnotation.add(AutoBind.class.getCanonicalName());//完整的类名
        return supportAnnotation;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();//表示支持最新的Java
    }


    //用于记录需要绑定的View的名称和对应的id
    private Map<TypeElement, Set<ViewInfo>> mToBindMap = new HashMap<>();

    /**
     * 处理注解标记的元素，并根据分类生成对应的辅助类
     * 以下是关于apt扫描文件时元素的几种类型简单介绍
     * from:https://juejin.cn/post/6844903923233341453
     * package com.zhpan.mannotation.factory;  //    PackageElement
     *
     * public class Circle {  //  TypeElement
     *
     *     private int i; //   VariableElement
     *     private Triangle triangle;  //  VariableElement
     *
     *     public Circle() {} //    ExecutableElement
     *
     *     public void draw(   //  ExecutableElement
     *                         String s)   //  VariableElement
     *     {
     *         System.out.println(s);
     *     }
     *
     *     #@Override
     *     public void draw() {    //  ExecutableElement
     *         System.out.println("Draw a circle");
     *     }
     * }
     *
     * @param set 支持的注解类型
     * @param roundEnvironment 表示apt的环境，可以通过它获取被注解标记的代码元素，即上面所述的三种元素
     * @return 是否交由本 Processor 处理
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (null != set && set.size() > 0) { //表示存在 AutoBind 注解，从而不用遍历该集合直接使用
            //获取被 AutoBind 注解标记的的所有元素
            Set<? extends Element>  elements = roundEnvironment.getElementsAnnotatedWith(AutoBind.class);

            //根据在不同的 Activity 标记的元素进行区分
            for(Element element: elements) {
                //因为AutoBind标记是类中的字段，简单起见，这里不对元素类型进行判定，直接默认是 VariableElement（字段元素）
                VariableElement variableElement = (VariableElement) element;
                //根据字段元素获取到被标记元素的类型元素，也就是注解作用字段的对应Activity的信息
                //（Enclosing 翻译为 包围围绕）
                TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
                //将 Activity 对应 注解标记的View 进行匹配，若是在同一个Activity 的 View，则放到同一个Set集合中
                Set<ViewInfo> viewInfos = mToBindMap.get(typeElement);
                if (null == viewInfos) { //若不存在对应集合，则创建一个空的集合
                    viewInfos = new HashSet<>();
                    mToBindMap.put(typeElement, viewInfos);
                }
                //获取字段元素的注解
                AutoBind annotation = variableElement.getAnnotation(AutoBind.class);
                //获取注解上设置的值,即View相关的R.id
                int valueId = annotation.value();
                //保存相关信息
                viewInfos.add(new ViewInfo(variableElement.getSimpleName(), valueId));
            }

            // 不同的Activity 生成不同的辅助类
            for(TypeElement typeElement: mToBindMap.keySet()) {
                //生成辅助类的代码文本
                String javaCode = generateJavaCode(typeElement);
                String helperClassName = typeElement.getQualifiedName() + "$$AutoBind";

                //将java文本转化为Java类
                //输出文件的位置在 build/source/apt 目录下
                try {
                    JavaFileObject jfo = filer.createSourceFile(helperClassName, typeElement);
                    Writer writer = jfo.openWriter();
                    writer.write(javaCode);
                    writer.flush();
                    writer.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 根据类型元素生成对应的辅助类，该类有存在 findViewByid 的相关操作方法
     * 生成的类文本大概是以下格式：
     * package me.tommy.apt_exercise;
     * import me.tommy.autoviewbind.IAutoBind;
     *
     * public class XxxActivity$$Autobind implements IAutoBind {
     *    @Override
     *    public void inject(Object target ) {
     * 		XxxActivity activity = (XxxActivity)target;
     * 		activity.tvText = activity.findViewById(xxxxxxx);
     *    }
     *
     * }
     *
     * @param typeElement
     * @return
     */
    private String generateJavaCode(TypeElement typeElement) {
        //注解标记View所在的类的类名
        String rawClassName = typeElement.getSimpleName().toString();
        //注解标记View所在的类的包名
        String packageName =  elementUtils
                .getPackageOf(typeElement).getQualifiedName().toString();
        //要生成的辅助类的名称
        String helperClassName = rawClassName + "$$AutoBind";

        //生成一个java文件的文本
        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(packageName).append(";\n");
        builder.append("import me.tommy.autoviewbind.IAutoBind;\n\n");
        builder.append("public class " ).append(helperClassName).append(" implements ").append("IAutoBind{\n");
        builder.append("\n");
        builder.append("\t@Override\n");
        builder.append("\tpublic void inject(Object target){\n");
        for (ViewInfo viewInfo:mToBindMap.get(typeElement)) {
            builder.append("\t\t");
            //强制类型转换
            builder.append(rawClassName).append(" activity = (").append(rawClassName).append(") target;\n");
            builder.append("\t\t");
            //赋值
            builder.append("activity.").append(viewInfo.getName()).append("=");
            builder.append("activity.findViewById(").append(viewInfo.getId()).append(");\n");
        }
        builder.append("\t}\n");
        builder.append('\n');
        builder.append("}\n");
        return builder.toString();
    }

}