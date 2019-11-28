package io.hyperfoil.tools.yaup.json;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;


//https://github.com/graalvm/graaljs/issues/44

public class NashornMapContext implements javax.script.ScriptContext{

   private Map<Object,Object> map;
   private ScriptContext parent;


   public NashornMapContext(Map<Object,Object> map){this(map,new SimpleScriptContext());}
   public NashornMapContext(Map<Object,Object> map,ScriptContext parent){
      this.map = map;
      this.parent = parent;
   }

   @Override
   public void setBindings(Bindings bindings, int scope) {
      parent.setBindings(bindings,scope);
   }

   @Override
   public Bindings getBindings(int scope) {
      return parent.getBindings(scope);
   }

   @Override
   public void setAttribute(String name, Object value, int scope) {
      parent.setAttribute(name,value,scope);
   }

   @Override
   public Object getAttribute(String name, int scope) {
      if(map.containsKey(name)){
         return map.get(name);
      }else{
         return parent.getAttribute(name,scope);
      }
   }

   @Override
   public Object removeAttribute(String name, int scope) {
      return parent.removeAttribute(name,scope);
   }

   @Override
   public Object getAttribute(String name) {
      if(map.containsKey(name)){
         return map.get(name);
      }else{
         return parent.getAttribute(name);
      }
   }

   @Override
   public int getAttributesScope(String name) {
      return 0;
   }

   @Override
   public Writer getWriter() {
      return parent.getWriter();
   }

   @Override
   public Writer getErrorWriter() {
      return parent.getErrorWriter();
   }

   @Override
   public void setWriter(Writer writer) {
      parent.setWriter(writer);
   }

   @Override
   public void setErrorWriter(Writer writer) {
      parent.setErrorWriter(writer);
   }

   @Override
   public Reader getReader() {
      return parent.getReader();
   }

   @Override
   public void setReader(Reader reader) {
      parent.setReader(reader);
   }

   @Override
   public List<Integer> getScopes() {
      return parent.getScopes();
   }
}
