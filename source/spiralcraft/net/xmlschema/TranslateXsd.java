//
// Copyright (c) 2009 Michael Toth
// Spiralcraft Inc., All Rights Reserved
//
// This package is part of the Spiralcraft project and is licensed under
// a multiple-license framework.
//
// You may not use this file except in compliance with the terms found in the
// SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
// at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
// Unless otherwise agreed to in writing, this software is distributed on an
// "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.net.xmlschema;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import spiralcraft.task.Chain;
import spiralcraft.task.Task;


import spiralcraft.data.core.AbstractCollectionType;
import spiralcraft.data.core.MetaType;
import spiralcraft.data.core.TypeImpl;
import spiralcraft.data.lang.DataReflector;
import spiralcraft.data.sax.AbstractFrameHandler;
import spiralcraft.data.sax.DataWriter;
import spiralcraft.data.spi.EditableArrayListAggregate;
import spiralcraft.data.spi.EditableArrayTuple;
import spiralcraft.data.util.StaticInstanceResolver;
import spiralcraft.data.Aggregate;
import spiralcraft.data.DataException;
import spiralcraft.data.Tuple;
import spiralcraft.data.Type;
import spiralcraft.data.TypeResolver;

import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;


/**
 * <p>Reads an XML-Data Schema and generates data Types and a SAX handler tree
 * </p>
 * 
 * <p>Currently experimental, as there is a large impedence mismatch
 *   between XML-Data Schema descriptions and normalized object oriented data
 *   models.
 * </p> 
 * 
 * 
 * @author mike
 *
 */

public class TranslateXsd
  extends Chain
{
  public static URI DATA_SAX_URI=URI.create("class:/spiralcraft/data/sax/");
  public static URI DATA_XSD_URI=URI.create("class:/spiralcraft/net/xmlschema/"); 
  
  public static URI STANDARD_TYPES_URI
    =URI.create("class:/spiralcraft/data/types/standard/");
  
  // Pojo for now
  public static URI XSD_URI=URI.create("http://www.w3.org/2001/XMLSchema");
  public static URI TRANSLATION_URI
    =URI.create("class:/spiralcraft/net/xmlschema/Translation");
  
  protected Type<Tuple> translationType;
  
  protected ThreadLocalChannel<Tuple> translation;
  
  private URI schemaURI;
  
  private URI targetURI;
  private URI outputLocation;
  
  private URI[] selectedURIs;
  
  private Type<Tuple> complexTypeType
    =Type.<Tuple>resolve(DATA_XSD_URI.resolve("ComplexType"));
  
  private Type<Tuple> simpleTypeType
    =Type.<Tuple>resolve(DATA_XSD_URI.resolve("SimpleType"));

  private Type<?> typeType
    =Type.resolve("class:/spiralcraft/data/types/meta/Type");
  private Type<?> fieldType
    =Type.resolve("class:/spiralcraft/data/types/meta/Field");
  
  private Type<?> anyType
    =Type.resolve("class:/spiralcraft/data/types/standard/Any");

  private Type<?> frameHandlerListType
    =Type.resolve("class:/spiralcraft/data/sax/FrameHandler.list");
  
  private Type<?> rootFrameType
    =Type.resolve("class:/spiralcraft/data/sax/RootFrame");
  private Type<?> tupleFrameType
    =Type.resolve("class:/spiralcraft/data/sax/TupleFrame");
//  private Type<?> containerFrameType
//    =Type.resolve("class:/spiralcraft/data/sax/ContainerFrame");
  private Type<?> aggregateFrameType
    =Type.resolve("class:/spiralcraft/data/sax/AggregateFrame");
  private Type<?> valueFrameType
    =Type.resolve("class:/spiralcraft/data/sax/ValueFrame");

  
  private Type<?> recursiveFrameType
    =Type.resolve("class:/spiralcraft/data/sax/RecursiveFrame");
  private Type<?> attributeBindingListType
    =Type.resolve("class:/spiralcraft/data/sax/AttributeBinding.list");
  private Type<?> attributeBindingType
    =Type.resolve("class:/spiralcraft/data/sax/AttributeBinding");
  
  public TranslateXsd()
    throws BindException,DataException
  {
    translationType=Type.resolve(TRANSLATION_URI);
    translation=new ThreadLocalChannel<Tuple>
        (DataReflector.<Tuple>getInstance(translationType)
        );
    
  }
  
  public void setSchemaURI(URI schemaURI)
  { this.schemaURI=schemaURI;
  }
  
  public URI getSchemaURI()
  { return schemaURI;
  }
  
  /**
   * The URI under which the types and handlers will be published, so that
   *   the resources can cross reference one another
   * 
   * @param targetURI
   */
  public void setTargetURI(URI targetURI)
  { this.targetURI=targetURI;
  }
  
  public void setSelectedURIs(URI[] selectedURIs)
  { this.selectedURIs=selectedURIs;
  }
    
  /**
   * The location of the directory into which the type and handler definitions
   *   will be generated.
   *   
   * @param outputURI
   */
  public void setOutputLocation(URI outputLocation)
  { this.outputLocation=outputLocation;
  }
  
  
  @Override
  public Task task()
  { return new TranslateTask();
  }
  
  @Override
  public void bindChildren(Focus<?> focus)
    throws BindException
  { 
    focus=focus.chain(translation);    
    super.bindChildren(focus);
  }
  
  protected class TranslateTask
    extends ChainTask
  {
    
    
    private HashMap<String,TypeRef> typeMap
      =new HashMap<String,TypeRef>();
    { addStandardTypes();
    }
    
    private Tuple schema;
    private TypeResolver typeResolver;
    private String targetNamespace;
    private Stack<HandlerRef> handlerStack=new Stack<HandlerRef>();
    
    protected void addStandardTypes()
    { 
      addStandardType("string","String");
      addStandardType("decimal","BigDecimal");
      addStandardType("date","Date");
      addStandardType("dateTime","Date");
      addStandardType("long","Long");
      addStandardType("boolean","Boolean");
      addStandardType("nonNegativeInteger","Integer");
      addStandardType("positiveInteger","Integer");
      addStandardType("int","Integer");
      addStandardType("duration","Long");
      addStandardType("anyURI","URI");
      
      // XXX We should have a string subtype for the NMTOKEN rules
      addStandardType("NMTOKEN","String");
    }
    
    protected void addStandardType(String xsdLocalName,String dataLocalName)
    { 
      TypeRef ref=new TypeRef();
      ref.typeName=AbstractFrameHandler.combineName
                    (XSD_URI.toString(), xsdLocalName);
      try
      {
        ref.dataType
          =Type.resolve
            (STANDARD_TYPES_URI.resolve(dataLocalName));
        ref.handlerTemplate=new EditableArrayTuple(valueFrameType);
        typeMap.put(ref.typeName,ref);
      }
      catch (DataException x)
      { throw new RuntimeException
          ("Error resolving standard type "+dataLocalName);
      }
    }
      
    @Override
    public void work()
      throws InterruptedException
    { 
      translation.push(new EditableArrayTuple(translationType));
      try
      {
      
        super.work();
        Tuple tt=translation.get();
        if (debug)
        { log.fine("Read "+tt.toText("| "));
        }
        generate(tt);
      }
      catch (Exception x)
      { 
        addException(x);
        return;
      }
      finally
      { translation.pop();
      }
      
    }
    
    @SuppressWarnings("unchecked")
    private void generate(Tuple translation)
      throws DataException,IOException
    { 
      typeResolver=TypeResolver.getTypeResolver();

      schema=(Tuple) translation.get("schema");
      targetNamespace=(String) schema.get("targetNamespace");
      if (debug)
      { log.fine("Target namespace is "+targetNamespace);
      }
      
      for (Tuple element : (Aggregate<Tuple>) schema.get("rootElements"))
      {
        if (selectedURIs!=null)
        {
          for (URI uri:selectedURIs)
          { 
            if ( URI.create(((String) element.get("elementName"))).equals(uri))
            { generateHandlerSet(element);
            }
          }
        }
        else
        { generateHandlerSet(element);
        }
      }
    }
    
    @SuppressWarnings("unchecked")
    // This is only called from the top level, not re-entrant
    private void generateHandlerSet(Tuple element)
      throws DataException,IOException
    {
      EditableArrayTuple rootFrame=new EditableArrayTuple(rootFrameType);
      EditableArrayListAggregate children=ensureChildren(rootFrame);
      
      if (targetNamespace!=null)
      { rootFrame.set("defaultURI",URI.create(targetNamespace));
      }
      
      HandlerRef handlerRef=new HandlerRef();
      handlerRef.handler=rootFrame;
      handlerStack.push(handlerRef);
      
      String elementType=(String) element.get("typeName");
      String elementName=(String) element.get("elementName");
      
      if (debug)
      { log.fine("Generating handler for "+elementName);
      }
      
      if (elementType!=null)
      { 
        if (debug)
        { log.fine("type is "+elementType);
        }
        TypeRef elementTypeRef=resolveType(elementType);
        if (elementTypeRef==null)
        { throw new DataException
            ("No type resolved for element "+elementName+" type "+elementType);
          
        }
        else
        {
          EditableArrayTuple childElement
            =makeHandlerFromTemplate(elementTypeRef);
          childElement.set("elementURI",elementName);
          children.add(childElement);
        }
      }
      else
      { 
      }
      

      
      handlerStack.pop();
      new DataWriter().writeToURI
        (outputLocation.resolve(elementName+"Root.frame.xml"),rootFrame);
      
      
      if (debug)
      {
        log.fine
          ( rootFrame.toText("| ")
          );
      }
      
    }
    
    private TypeRef resolveType(String elementType)
      throws DataException,IOException
    {
      
      TypeRef ref=typeMap.get(elementType);
      if (ref==null)
      { 
        
        Tuple xsdType=findXsdType(elementType);
        if (xsdType==null)
        { throw new DataException("Type "+elementType+" not found");
        }
        ref=createType(elementType,xsdType);
      }
      return ref;
    }
    
    @SuppressWarnings("unchecked")
    private TypeRef createType(String elementType,Tuple xsdType)
      throws DataException,IOException
    {
      if (debug)
      { log.fine("Creating type for "+elementType);
      }
      
      TypeRef ref=new TypeRef();
      ref.xsdType=xsdType;
      ref.typeName=elementType;
      typeMap.put(elementType,ref);
      
      
      // Determine the type model from the nesting pattern
      if (xsdType.getType()==complexTypeType)
      { 
        Aggregate<Tuple> elements=(Aggregate<Tuple>) xsdType.get("elements");
        Aggregate<Tuple> attributes=(Aggregate<Tuple>) xsdType.get("attributes");
        Aggregate<Tuple> choices=(Aggregate<Tuple>) xsdType.get("choices");
        
        if (elements!=null)
        {
          if (attributes==null
              && choices==null
              && elements.size()==1 
              && Boolean.TRUE.equals(elements.get(0).get("plural"))
              && elements.get(0).get("inlineType")!=null
              )
          { 
            // A type with a single, plural element and no attributes
            //   maps to a simple list of the type mapped to the plural element
            createDeclaredListType
              (ref,(Tuple) elements.get(0).get("inlineType"));
          }
          else
          { createComplexType(ref);
          }
        }
        else if (attributes!=null)
        { createComplexType(ref);
        }
        else if (choices!=null)
        { 
          // XXX Handle a single plural choice situation as an Any.list
          createComplexType(ref);
        }
        else
        { 
          log.warning("No elements, attributes or choices found in "
            +ref.typeName);
        }
      }
      else
      { createSimpleType(ref);
      }
      return ref;
    }

    /**
     * A top level complex type that represents a .list type in data
     * 
     * We need to persist the component type, but we need to give it a
     *   name 
     * 
     * @param ref
     */
    @SuppressWarnings("unchecked")
    private void createDeclaredListType(TypeRef ref,Tuple inlineType)
      throws DataException,IOException
    {
      String unitTypeName=ref.typeName+"Unit";
      if (ref.typeName.endsWith("s"))
      { unitTypeName=ref.typeName.substring(0,ref.typeName.length()-1);
      }
      ref.unitTypeRef=createType(unitTypeName,inlineType);
      ref.dataType
        =new AbstractCollectionType
          (typeResolver
          ,ref.unitTypeRef.dataType
          ,URI.create(ref.unitTypeRef.dataType.getURI().toString()+".list")
          ,List.class
          );
      ref.handlerTemplate=new EditableArrayTuple(aggregateFrameType);
      ensureChildren(ref.handlerTemplate).add(ref.unitTypeRef.handlerTemplate);
      
// extendHandler factor
//      ((EditableArrayListAggregate<Tuple>) ref.handlerTemplate.get("children"))
//        .add(extendHandler(ref.unitTypeRef));

     

      
    }
    
    @SuppressWarnings("unchecked")
    private void createSimpleType(TypeRef ref)
      throws DataException,IOException
    {
      String itemTypeName=(String) ref.xsdType.get("listItemTypeName");
      if (itemTypeName!=null)
      {
        TypeRef itemTypeRef=resolveType(itemTypeName);
        ref.dataType=Type.getAggregateType(itemTypeRef.dataType);

      }
      
      ref.handlerTemplate=new EditableArrayTuple(valueFrameType);
      
      String baseTypeName=(String) ref.xsdType.get("baseTypeName");
      if (baseTypeName!=null)
      {
        ref.baseTypeRef=resolveType(baseTypeName);
        ref.dataType=ref.baseTypeRef.dataType;
        // XXX This shortcuts enums, etc.... fix later
      }
    }
    
    @SuppressWarnings("unchecked")
    private void createComplexType(TypeRef ref)
      throws DataException,IOException
    {
      
      
      String typeLocalName=AbstractFrameHandler.localName(ref.typeName);
      if (typeLocalName.endsWith("Type"))
      { typeLocalName=typeLocalName.substring(0,typeLocalName.length()-4);
      }
      if (Character.isLowerCase(typeLocalName.charAt(0)))
      { 
        typeLocalName
          =Character.toUpperCase
            (typeLocalName.charAt(0))+typeLocalName.substring(1);
      }
      
      URI typeURI=targetURI.resolve(typeLocalName);
      if (debug)
      { log.fine("typeURI is "+typeURI+" for "+ref.typeName);
      }
      // Pre-create the associated type so it can be cross-referenced 
      TypeImpl typeImpl=new TypeImpl(TypeResolver.getTypeResolver(),typeURI);
      ref.dataType=typeImpl;
      
      
      EditableArrayTuple dataTypeDecl
        =new EditableArrayTuple
          (typeType);
      
      ref.dataTypeDecl=dataTypeDecl;

      
      // Generate fields
      EditableArrayListAggregate<Tuple> fieldDecls
        =new EditableArrayListAggregate<Tuple>(Type.getAggregateType(fieldType));
      

      
      ref.handlerTemplate=new EditableArrayTuple(tupleFrameType);
      
      // Account for type extension construct
      String baseTypeName=(String) ref.xsdType.get("baseTypeName");
      if (baseTypeName!=null)
      {
        if (debug)
        { log.fine("Resolving base type "+baseTypeName+" for "+ref.typeName);
        }
        TypeRef baseType=resolveType(baseTypeName);
        if (baseType!=null && baseType.dataType!=null)
        {
          ref.baseTypeRef=baseType;
          if (baseType.dataType.isPrimitive())
          { 
            Tuple fieldDecl=generateField("value",baseType.dataType,false);
            if (fieldDecl!=null)
            { 
              fieldDecls.add(fieldDecl);
              ref.handlerTemplate.set
                ("textBinding",Expression.create
                   ("[:"+typeURI+"]."+(String) fieldDecl.get("name"))
                );
              
            }
            
          }
          else
          {             
            ref.handlerTemplate=extendHandler(baseType);
            dataTypeDecl.set("archetype",typeRefTuple(baseType.dataType));
          }
        }
        else
        { throw new DataException
            ("baseTypeName '"+baseTypeName+"' not found for "+ref.typeName);
        }        
      }

      ref.handlerTemplate.set("type",typeRefTuple(typeImpl));
      

      
      ref.fieldMap=new HashMap<String,Tuple>();
     
      
      if (ref.xsdType.get("elements")!=null)
      {
        
        // Create a field for each element
        for (Tuple typeElement: (Aggregate<Tuple>) ref.xsdType.get("elements"))
        { 
          Tuple fieldDecl=generateFieldFromElement(typeElement);
          if (fieldDecl!=null)
          { 
            fieldDecls.add(fieldDecl);
            ref.fieldMap.put((String) typeElement.get("elementName"),fieldDecl);
          }
          else
          { 
            log.warning("Element "+typeElement+" in TypeRef "+ref
                        +" did not generate a field");
          }
        }      
      }
      
      
      
      if (ref.xsdType.get("attributes")!=null)
      {
      
        if (ref.handlerTemplate.get("attributeBindings")==null)
        {
          ref.handlerTemplate.set
            ("attributeBindings"
            ,new EditableArrayListAggregate<Tuple>(attributeBindingListType)
            );
        }
        // Create a field for each attribute
        for (Tuple typeAttribute: (Aggregate<Tuple>) ref.xsdType.get("attributes"))
        { 
          Tuple fieldDecl=generateFieldFromAttribute(typeAttribute,ref);
          if (fieldDecl!=null)
          { 
            fieldDecls.add(fieldDecl);
          }        
        }
      }

      if (ref.xsdType.get("choices")!=null)
      {
      
        // Create fields for choices
        for (Tuple choice: (Aggregate<Tuple>) ref.xsdType.get("choices"))
        { 
          if (Boolean.TRUE.equals(choice.get("plural")))
          {
            if (choice.get("elements")!=null)
            {
              StringBuffer fieldName=new StringBuffer();
              ArrayList<String> elementNames=new ArrayList<String>();
              // Create a field for each element
              for (Tuple typeElement: (Aggregate<Tuple>) choice.get("elements"))
              { 
                
                String elementName=(String) typeElement.get("elementName");
                elementNames.add(elementName);
                if (fieldName.length()>0)
                { 
                  fieldName.append("And");
                  elementName
                    =elementName.substring(0,1).toUpperCase()
                    +elementName.substring(1);
                }
                else
                { 
                  elementName
                    =elementName.substring(0,1).toLowerCase()
                    +elementName.substring(1);
                  
                }
                fieldName.append(elementName);
                
                if (typeElement.get("typeName")!=null)
                { 
                  @SuppressWarnings("unused")
                  // We don't need to use this right now, we just need to
                  //   generate it. If we implement disjoint union Types
                  //   we'll use the types.
                  TypeRef elementType
                    =resolveType((String) typeElement.get("typeName"));
                }
                
              } 
              
              Tuple fieldDecl
                =generateField(fieldName.toString(),anyType,true);
                
              fieldDecls.add(fieldDecl);
              
              for (String elementName:elementNames)
              { ref.fieldMap.put(elementName,fieldDecl);
              }
                        
            }
            
          }
          else
          {
            
            if (choice.get("elements")!=null)
            {
              // Create a field for each element
              for (Tuple typeElement: (Aggregate<Tuple>) choice.get("elements"))
              { 
                Tuple fieldDecl=generateFieldFromElement(typeElement);
                if (fieldDecl!=null)
                { 
                  fieldDecls.add(fieldDecl);
                  ref.fieldMap.put
                    ((String) typeElement.get("elementName")
                    ,fieldDecl
                    );        
                }
                else
                { 
                  log.warning("Element "+typeElement+" in TypeRef "+ref
                        +" did not generate a field");
                }                
              }      
            }
            
          }
       
        }
      }
      
      
      dataTypeDecl.set("fields",fieldDecls);

      // Write out declaration
      if (debug)
      { log.fine("Created type "+typeURI+": "+ref.dataTypeDecl.toText("| "));
      }
      new DataWriter().writeToURI
        (outputLocation.resolve(typeLocalName+".type.xml"),ref.dataTypeDecl);

      // Instantiate the type for cross ref

      typeImpl=(TypeImpl) typeType.fromData
          (ref.dataTypeDecl
          ,new StaticInstanceResolver(typeImpl)
          );

      /// XXX Register here? We shouldn't need to in the generation phase
      //typeResolver.register(typeURI,typeImpl);


    }
    
    private Tuple generateFieldFromElement
      (Tuple element)
      throws DataException,IOException
    {
      String ref;
      
      while ( (ref=(String) element.get("ref"))!=null)
      { 
        element=findElementRef(ref);
        if (element==null)
        { 
          throw new DataException
            ("Element reference '"+ref+"' does not resolve");
        }
      }
      
      String typeName=(String) element.get("typeName");
      Tuple inlineType=(Tuple) element.get("inlineType");
      TypeRef type=null;
      
      if (typeName!=null)
      {
        type=resolveType(typeName);
        if (type==null)
        { throw new DataException("TypeName '"+typeName+"' not found");
        }
        
      }
      else if (inlineType!=null)
      {
        type=createType((String) element.get("elementName"),inlineType);
      }
      else
      { 
        log.warning("Element '"+element.get("elementName")+"' is missing a" +
        		" type reference or an inline type"
            );
        return null;
        
      }
      
      if (type.dataType!=null)
      {
        Tuple field=generateField
          ((String) element.get("elementName")
          ,type.dataType
          ,Boolean.TRUE.equals(element.get("plural"))
          );
        
// Due to recursion issues, we can't add child templates here        
//        EditableArrayTuple child=new EditableArrayTuple(type.handlerTemplate);
//        child.set("elementURI",element.get("elementName"));
//        ensureChildren(parent.handlerTemplate).add(child);

        return field;
      }
      else
      { throw new DataException("TypeName '"+typeName+"' has no dataType");
      }
      

      
    }

    @SuppressWarnings("unchecked")
    private Tuple generateFieldFromAttribute
      (Tuple attribute,TypeRef parent)
      throws DataException,IOException
    {
      String typeName=(String) attribute.get("typeName");
      if (typeName!=null)
      {
        TypeRef type=resolveType(typeName);
        if (type!=null && type.dataType!=null)
        {
          
          EditableArrayListAggregate<Tuple> bindings
            =(EditableArrayListAggregate<Tuple>) 
              parent.handlerTemplate.get("attributeBindings");
          
          Tuple field=generateField
            ((String) attribute.get("name")
            ,type.dataType
            ,false
            );

          EditableArrayTuple binding
            =new EditableArrayTuple(attributeBindingType);
          binding.set("name",attribute.get("name"));
          binding.set("target", Expression.create((String) field.get("name")));

          bindings.add(binding);
          
          return field;
          
      
        }
        else
        { 
          if (type==null)
          { throw new DataException("TypeName '"+typeName+"' not found");
          }
          else
          { throw new DataException("TypeName '"+typeName+"' has no dataType");
          }
        }
      }
      log.warning("Attribute '"+attribute.get("name")+"' missing public type");
      return null;
    }
    

    
    @SuppressWarnings("unchecked")
    private Tuple generateField(String name,Type dataType,boolean plural)
      throws DataException
    {
      EditableArrayTuple fieldDecl=new EditableArrayTuple(fieldType);
      if (plural)
      { fieldDecl.set("name",name+"List");
      }
      else
      { fieldDecl.set("name",name);
      }     
      
      if (plural && !dataType.isAggregate())
      {   
        dataType
          =new AbstractCollectionType
            (typeResolver
            ,dataType
            ,URI.create(dataType.getURI().toString()+".list")
            ,List.class
            );
      }
      fieldDecl.set("type",typeRefTuple(dataType));
      return fieldDecl;
      
    }
    
    @SuppressWarnings("unchecked")
    private Tuple findXsdType(String elementType)
      throws DataException
    { 
      for (Tuple type : (Aggregate<Tuple>) schema.get("types"))
      { 
        String compareName=(String) type.get("typeName");
        if (targetNamespace!=null)
        { 
          compareName
            =AbstractFrameHandler.combineName
                    (targetNamespace,compareName);
        }
        
        if (elementType.equals(compareName))
        { return type;
        }
        else
        { // log.fine(compareName+"!="+elementType);
        }
      }
      return null;
    }
    
    @SuppressWarnings("unchecked")
    private Tuple findElementRef(String elementName)
      throws DataException
    {
      for (Tuple element : (Aggregate<Tuple>) schema.get("rootElements"))
      { 
        String compareName=(String) element.get("elementName");
        if (targetNamespace!=null)
        { 
          compareName
            =AbstractFrameHandler.combineName
                    (targetNamespace,compareName);
        }
        
        if (elementName.equals(compareName))
        { return element;
        }
        else
        { // log.fine(compareName+"!="+elementType);
        }
      }
      return null;      
    }
    
    /** 
     * Create a new handler for a subtype by copying all the attributes and
     *   children of a base type
     */
    @SuppressWarnings("unchecked")
    private EditableArrayTuple extendHandler(TypeRef baseType)
      throws DataException,IOException
    {
      // EditableArrayTuple handler=makeHandler(baseType);
      EditableArrayTuple handler=baseType.handlerTemplate;
      
      EditableArrayTuple extension
        =new EditableArrayTuple(handler.getType());
      if (handler.get("attributeBindings")!=null)
      {
        if (debug)
        { log.fine("Copying attributeBindings from "+baseType);
        }
        extension.set
          ("attributeBindings"
          ,new EditableArrayListAggregate
            ((Aggregate) handler.get("attributeBindings"))
          );
      }
      if (handler.get("children")!=null)
      {
        if (debug)
        { log.fine("Copying children from "+baseType);
        }
        extension.set
          ("children"
          ,new EditableArrayListAggregate
            ((Aggregate) handler.get("children"))
          );
      }
      if (debug)
      { 
        log.fine
          ("Extended handler for "
          +baseType.typeName+": "+extension.toString()
          );
      }
      return extension;
      
    }
    
    /**
     * Make a handler for the specified type reference using the 
     *   pre-constructed template as a prototype
     * 
     * @param ref
     * @return
     * @throws DataException
     * @throws IOException
     */
    private EditableArrayTuple makeHandlerFromTemplate(TypeRef ref)
      throws DataException,IOException
    {
      if (ref.handlerTemplate==null)
      { 
        throw new DataException
          ("No handler template for "+ref.typeName);
      }
      
      EditableArrayTuple handler;
      if (ref.xsdType==null || ref.xsdType.getType().equals(simpleTypeType))
      { handler=makeSimpleHandler(ref);
      }
      else
      { 
        
        handler=makeComplexHandler(ref);
      }
      if (debug)
      { log.fine("Handler for "+ref+" is "+handler.toString());
      }
      return handler;
    }
    
    
    private EditableArrayTuple makeSimpleHandler(TypeRef ref)
      throws DataException,IOException
    {
      EditableArrayTuple handler
        =new EditableArrayTuple(ref.handlerTemplate);

      return handler;
      
    }
    
    private EditableArrayTuple makeComplexHandler(TypeRef ref)
      throws DataException,IOException
    {

      if (debug)
      { log.fine("Making complex handler for "+ref.dataType+", "+ref.typeName);
      }
    

      HandlerRef handlerRef=new HandlerRef();
      handlerRef.handler=new EditableArrayTuple(ref.handlerTemplate);
      handlerRef.typeName=ref.typeName;
      handlerStack.push(handlerRef);
      
      combineHandlerChildren(ref,handlerRef);
      
      
      handlerStack.pop();
      return (EditableArrayTuple) handlerRef.handler;
    
    }
    
    @SuppressWarnings("unchecked")
    private void combineHandlerChildren(TypeRef ref,HandlerRef handlerRef)
      throws DataException,IOException
    {
      EditableArrayTuple handler=(EditableArrayTuple) handlerRef.handler;

      EditableArrayListAggregate children
        =ensureChildren(handler);      
      // Account for type extension construct
      
      if (ref.baseTypeRef!=null && ref.baseTypeRef.xsdType!=null)
      { 
        if (debug)
        { log.fine("Adding base children for "+ref.baseTypeRef);
        }
        combineHandlerChildren(ref.baseTypeRef,handlerRef);
      }

      
      if (ref.xsdType.get("elements")!=null)
      {
        // Create a handler for each element
        for (Tuple element: (Aggregate<Tuple>) ref.xsdType.get("elements"))
        { 
          Tuple childHandler=completeHandlerForElement(element,ref,handler);
          if (childHandler!=null)
          { children.add(childHandler);
          }

        }
      }
      
      
      if (ref.xsdType.get("choices")!=null)
      {

        for (Tuple choice: (Aggregate<Tuple>) ref.xsdType.get("choices"))
        {         
          // Do we really differentiate between plural/singular here?
          if (choice.get("elements")!=null)
          {
            for (Tuple element: (Aggregate<Tuple>) choice.get("elements"))
            { 
              Tuple childHandler=completeHandlerForElement(element,ref,handler);
              if (childHandler!=null)
              { children.add(childHandler);
              }
            }
            
          }
        }
      }
    }
    
    
    /**
     * Complete the handler for an Element, within the context of 
     *   its container
     *   
     * @param element The element declaration to complete a handler for
     * @param containerRef The TypeRef for whatever contains this element
     * @param handler
     * @return
     * @throws DataException
     * @throws IOException
     */
    public Tuple completeHandlerForElement
      (Tuple element
      ,TypeRef containerRef
      ,EditableArrayTuple handler
      )
      throws DataException,IOException
    {
      String elementType=(String) element.get("typeName");
      String elementName=(String) element.get("elementName");
      Tuple inlineType=(Tuple) element.get("inlineType");
      
      
      
      if (debug)
      { log.fine("Generating handler for "+elementName);
      }

      TypeRef elementTypeRef=null;
      if (elementType!=null)
      {
        if (debug)
        { log.fine("type for '"+elementName+"' is "+elementType);
        }
        elementTypeRef=resolveType(elementType);
        if (elementTypeRef==null)
        { 
          throw new DataException
            ("Could not resolve element "+elementName+" type "+elementType);
        }        
      }
      else if (inlineType!=null)
      { 
        log.fine("Creating in-line type for '"+elementName+"'");
        
        // Name the inline type after the element name
        elementTypeRef=createType(elementName,inlineType);
        
      }
      
      
      if (elementTypeRef!=null)
      { 
        
            
        // Handle recursive cases
        HandlerRef backRef=findInStack(elementTypeRef.typeName);
        if (backRef!=null)
        {
          log.fine("Found in stack "+backRef.handler);
          String id=(String) backRef.handler.get("id");
          if (id==null)
          { 
            // We are at the top of the recursive chain
            
            handler.set("id",elementName);
            log.fine("Setting id="+elementName);
            // Drop through and create the type once more for back ref
            backRef=null;
          }
          else 
          {
            EditableArrayTuple recursiveHandler
              =new EditableArrayTuple(recursiveFrameType);
            recursiveHandler.set("frameId",id);
            return recursiveHandler;
          }
        }
            
        if (backRef==null)
        {
          EditableArrayTuple childHandler
            =makeHandlerFromTemplate(elementTypeRef);

          String containingType=containerRef.dataType.getURI().toString();
          childHandler.set("elementURI",elementName);
              
          if (containerRef.fieldMap!=null)
          { 
            // This element corresponds to a value of a field
            Tuple fieldDecl=containerRef.fieldMap.get(elementName);
            String fieldName=(String) fieldDecl.get("name");
            String reference
              =containingType!=null
              ?("[:"+containingType+"].")+fieldName
              :fieldName
              ;
          
            if (childHandler.getType().equals(tupleFrameType))
            {
              if ( getFieldDeclType(fieldDecl).isAggregate() )
              { 
                childHandler.set
                  ("container", Expression.create(reference));
              }
              else
              { 
                childHandler.set
                  ("assignment", Expression.create(reference));
              }
            }
            else if (childHandler.getType().equals(valueFrameType))
            { 
              if (getFieldDeclType(fieldDecl).isAggregate())
              { 
                childHandler.set("container",Expression.create(reference));
                childHandler.set
                  ("type"
                  ,typeRefTuple(getFieldDeclType(fieldDecl).getContentType())
                  );
              }
              else
              { childHandler.set("assignment",Expression.create(reference));
              }
            
            }
            else if (childHandler.getType().equals(aggregateFrameType))
            {
              childHandler.set("assignment", Expression.create(reference));
            }
            
            return childHandler;
          }
          else if (containerRef.dataType.isAggregate())
          { 
            // This element corresponds to an item in a collection
            String reference="[:"+containingType+"]";
            childHandler.set("container", Expression.create(reference));
            
            return childHandler;
          }
        }
      }
      return null;
      
    }
    
    
    public Type<?> getFieldDeclType(Tuple fieldDecl)
      throws DataException
    { 
      Tuple typeRef=(Tuple) fieldDecl.get("type");
      MetaType metaType=(MetaType) typeRef.getType();
      return metaType.fromData(typeRef,null);
    }
    
    public HandlerRef findInStack(String typeName)
    { 
      for (HandlerRef ref:handlerStack)
      {
        if (ref.typeName!=null && ref.typeName.equals(typeName))
        { return ref;
        }
      }
      return null;
    }
    
    @SuppressWarnings("unchecked")
    public EditableArrayListAggregate<Tuple>
      ensureChildren(EditableArrayTuple frameHandler)
      throws DataException
    {
      EditableArrayListAggregate children
        =(EditableArrayListAggregate) frameHandler.get("children");
      if (children==null)
      { 
        children=new EditableArrayListAggregate(frameHandlerListType);
        frameHandler.set("children",children);
      }
      return children;
    }

    
  }
  
  

  @SuppressWarnings("unchecked")
  class TypeRef
  {
    public String typeName;
    public Tuple xsdType;
    public Type dataType;
    public Tuple dataTypeDecl;
    public TypeRef unitTypeRef;
    public TypeRef baseTypeRef;
    public EditableArrayTuple handlerTemplate;
    public HashMap<String,Tuple> fieldMap;
    
    @Override
    public String toString()
    {
      return super.toString()+": ["
        +"\r\n  typeName="+typeName
        +"\r\n  xsdType="+xsdType
        +"\r\n  dataType="+dataType
        +"\r\n  unitTypeRef="+unitTypeRef
        +"\r\n  baseTypeRef="+baseTypeRef
        +"]";
    }

  }
  
  class HandlerRef
  {
    public String typeName;
    public Tuple handler;
  }
  
  
  private Tuple typeRefTuple(Type<?> type)
    throws DataException
  { 
    if (type.isLinked())
    { 
        
      // log.fine("MetaType for "+type+" is "+type.getMetaType());
      return new EditableArrayTuple(type.getMetaType());
    }
      
    TypeImpl<?> metaType=new MetaType(type);
    metaType.link();
    // log.fine("MetaType for "+type+" is "+metaType);
    return new EditableArrayTuple(metaType);
  }  
}
