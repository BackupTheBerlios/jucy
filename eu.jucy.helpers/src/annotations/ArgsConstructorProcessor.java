package annotations;




import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;



@SupportedAnnotationTypes("annotations.ArgsConstructor")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ArgsConstructorProcessor extends AbstractProcessor {

	
	@Override
	public boolean process(Set<? extends TypeElement> annotations,RoundEnvironment env) {

		for (TypeElement type : annotations) {
			processArgsConstructorClasses(env, type);
		}
		return true;

	}
	
	private void processArgsConstructorClasses(RoundEnvironment env, TypeElement type) {
		for (Element element : env.getElementsAnnotatedWith(type)) {
			processClass(element);
		}
		
	}

	
	private void processClass(Element element) {
		
		String actionName = ArgsConstructor.class.getName();
		AnnotationValue action = null;
		for (AnnotationMirror am : processingEnv.getElementUtils().getAllAnnotationMirrors(element)) {
			 if ( actionName.equals(am.getAnnotationType().toString() ) ) {
				 for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet() ) {
					 if ( "value".equals(entry.getKey().getSimpleName().toString() ) ) {
						 action = entry.getValue();
						 break;
					 }

				 }

			 }
		}
		
		if (action == null) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
					"Class " + element + " lacks an annotation with required args",element);
			return;
		}
		
		List<TypeMirror> mirrors = new ArrayList<TypeMirror>();
		for (Object val: (List<?>)action.getValue()) {
			AnnotationValue v= (AnnotationValue)val;
			TypeMirror m =(TypeMirror)v.getValue(); 
			mirrors.add(m);
		}
		
		if (!doesClassContainArgsConstructor(element,mirrors)) {
			String s = "";
			for (TypeMirror tm:mirrors) {
				if (!s.isEmpty()) {
					s+=",";
				}
				s += tm.toString();
				
			}
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
					"Class " + element + " lacks a public constructor with args: "+s,element);
		} else {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processed type: "+element);
		}
	}

	private boolean doesClassContainArgsConstructor(Element el,List<TypeMirror> annotTypes) {
		
		for (Element subelement : el.getEnclosedElements()) {
			if (subelement.getKind() == ElementKind.CONSTRUCTOR &&
					subelement.getModifiers().contains(Modifier.PUBLIC)) {
				TypeMirror mirror = subelement.asType();
				
				
				
				if (mirror.accept(argsVisitor, annotTypes)) return true;
			}
		}
		return false;
	}

	private final TypeVisitor<Boolean, List<TypeMirror>> argsVisitor =
		new SimpleTypeVisitor6<Boolean, List<TypeMirror>>() {
		public Boolean visitExecutable(ExecutableType t, List<TypeMirror> annotatedTypes) {
			
			List<? extends TypeMirror> types = t.getParameterTypes();
			if (annotatedTypes.size() != types.size()) {
				return false;
			}
			Types tutil = processingEnv.getTypeUtils();
			for (int i = 0 ; i < types.size(); i++) {
				
				TypeMirror test = tutil.erasure(types.get(i));//because same type bad Map<String,String> != Map
				TypeMirror expected = tutil.erasure(annotatedTypes.get(i));
		
				if (!tutil.isAssignable(expected, test)) { 
					return false;
				}
			}
			return true;
		}
	};


}
