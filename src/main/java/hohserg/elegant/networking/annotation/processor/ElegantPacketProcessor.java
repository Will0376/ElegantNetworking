package hohserg.elegant.networking.annotation.processor;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;
import hohserg.elegant.networking.api.ServerToClientPacket;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Set;

@AutoService(Processor.class)
public class ElegantPacketProcessor extends AbstractProcessor {
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ElegantPacket.class)) {
            switch (annotatedElement.getKind()) {
                case CLASS:
                    TypeElement typeElement = (TypeElement) annotatedElement;

                    if (annotatedElement.getModifiers().contains(Modifier.PUBLIC)) {
                        if (havePacketInterfaces(typeElement)) {
                            note("Found elegant packet class: " + annotatedElement.asType() + " interfaces: " + typeElement.getInterfaces());
                        } else
                            error("The elegant packet class must implement ClientToServerPacket or ServerToClientPacket");


                    } else
                        error("The elegant packet class must be public");

                    break;
                default:
                    error("@ElegantPacket can be applied only to classes");

            }
        }
        return false;
    }

    private boolean havePacketInterfaces(TypeElement typeElement) {
        TypeElement currentClass = typeElement;
        while (true) {
            for (TypeMirror anInterface : currentClass.getInterfaces()) {
                note(anInterface.toString());
                if (anInterface.toString().equals(ClientToServerPacket.class.getName()) || anInterface.toString().equals(ServerToClientPacket.class.getName()))
                    return true;
            }
            TypeMirror superClassType = currentClass.getSuperclass();

            if (superClassType.getKind() == TypeKind.NONE)
                return false;

            currentClass = (TypeElement) typeUtils.asElement(superClassType);
        }
    }

    private void note(String msg) {
        messager.printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void error(String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(ElegantPacket.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
}
