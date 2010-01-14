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
import spiralcraft.data.Field;
import spiralcraft.data.Tuple;
import spiralcraft.data.Type;
import spiralcraft.data.TypeNotFoundException;
import spiralcraft.data.TypeResolver;

import spiralcraft.lang.spi.ThreadLocalChannel;
import spiralcraft.lang.BindException;
import spiralcraft.lang.Expression;
import spiralcraft.lang.Focus;
import spiralcraft.log.Level;


/**
 * <p>Reads an XML-Data Schema and generates data Types and a SAX handler tree
 * </p>
 * 
 * <p>Currently experimental, due to the "impedance mismatch"
 *   between XML-Data Schema descriptions and normalized object oriented data
 *   models.
 * </p> 
 * 
 * 
 * @author mike
 *
 */

public class TranslateXsd
  extends Chain<Void,Void>
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
  private URI baseTypeNamespaceURI;
  private URI outputLocation;
  
  private URI[] selectedURIs;
  private URI[] errorElementURIs;
  
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
  
  public void setErrorElementURIs(URI[] errorElementURIs)
  { this.errorElementURIs=errorElementURIs;
  }
  
  public void setErrorElementURI(URI errorElementURI)
  { this.errorElementURIs=new URI[] {errorElementURI};
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
  
  /**
   * <p>The URI for the namespace that contains optional base types and
   *   handlers for the types and handlers that will be automatically generated
   *   by the XSD translation.
   * </p>
   *   
   * <p>If a type or handler with the same local name as a generated type
   *   or handler exists in this namespace, the generated type or handler
   *   will extend (for types) or reference (for handlers) the found type
   *   or handler. 
   * </p>
   * 
   * <p>If this property is null, no base types or handlers will be resolved
   * </p>
   * 
   * @param baseTypeNamespaceURI
   */
  public void setBaseTypeNamespaceURI(URI baseTypeNamespaceURI)
  { this.baseTypeNamespaceURI=baseTypeNamespaceURI;
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
    if (baseTypeNamespaceURI!=null
        && !baseTypeNamespaceURI.isAbsolute()
        )
    { 
      baseTypeNamespaceURI
        =URI.create("context:/").resolve(baseTypeNamespaceURI);
    }
    super.bindChildren(focus);
  }
  
  protected class TranslateTask
    extends ChainTask
  {
    
    
    private HashMap<String,TypeMapping> typeMap
      =new HashMap<String,TypeMapping>();
    { addStandardTypes();
    }
    
    private HashMap<String,Tuple> schemaTypeMap
      =new HashMap<String,Tuple>();
    
    private HashMap<String,Tuple> schemaElementMap
      =new HashMap<String,Tuple>();
    
    private Tuple schema;
    private TypeResolver typeResolver;
    private String targetNamespace;
    private Stack<HandlerRef> handlerStack=new Stack<HandlerRef>();
    
    protected void addStandardTypes()
    { 
      addStandardType("string","String");
      addStandardType("decimal","BigDecimal");
      addStandardType("date","Date");
      addStandardType("long","Long");
      addStandardType("boolean","Boolean");
      addStandardType("nonNegativeInteger","Integer");
      addStandardType("positiveInteger","Integer");
      addStandardType("int","Integer");
      addStandardType("anyURI","URI");
      
      // XXX We should have a string subtype for the NMTOKEN rules
      addStandardType("NMTOKEN","String");

      addMappedType("dateTime","class:/spiralcraft/net/xmlschema/types/Date");
    
      addMappedType("duration","class:/spiralcraft/net/xmlschema/types/Duration");
      addMappedType("boolean","class:/spiralcraft/net/xmlschema/types/Boolean");
    
    }
    
    protected void addMappedType(String xsdLocalName,String typeURI)
    {
      TypeMapping ref=new TypeMapping();
      ref.typeName=AbstractFrameHandler.combineName
                    (XSD_URI.toString(), xsdLocalName);
      try
      {
        ref.dataType
          =Type.resolve
            (typeURI);
        ref.handlerTemplate=new EditableArrayTuple(valueFrameType);
        typeMap.put(ref.typeName,ref);
      }
      catch (DataException x)
      { throw new RuntimeException
          ("Error resolving type "+typeURI,x);
      }
    }
      
    protected void addStandardType(String xsdLocalName,String dataLocalName)
    { 
      TypeMapping ref=new TypeMapping();
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
        if (getException()!=null)
        { return;
        }
        
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
    /**
     * Generate artifacts for a whole xsd
     */
    private void generate(Tuple translation)
      throws DataException,IOException
    { 
      typeResolver=TypeResolver.getTypeResolver();

      schema=(Tuple) translation.get("schema");
      targetNamespace=(String) schema.get("targetNamespace");
      
      mapSchemaTypes((Aggregate) schema.get("types"));
      mapSchemaElements((Aggregate) schema.get("rootElements"));
      
 
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
        { 
          generateHandlerSet(element);
        }
      }
      
    }
    
    public void mapSchemaTypes(Aggregate<Tuple> typeList)
      throws DataException
    { 
      for (Tuple type : typeList)
      { 
        String typeName=(String) type.get("typeName");
        
        if (targetNamespace!=null 
            && !URI.create(typeName).isAbsolute()
           )
        { 
          typeName
            =AbstractFrameHandler.combineName
                    (targetNamespace,typeName);
        }        
        schemaTypeMap.put(typeName, type);
        if (debug)
        { log.fine("Mapped type "+typeName+" -> "+type);
        }
      }
    }
    
    public void mapSchemaElements(Aggregate<Tuple> elementList)
      throws DataException
    {
      for (Tuple element : elementList)
      { 
        String elementName=(String) element.get("elementName");
        
        if (targetNamespace!=null 
            && !URI.create(elementName).isAbsolute()
           )
        { 
          elementName
            =AbstractFrameHandler.combineName
                    (targetNamespace,elementName);
        }        
        schemaElementMap.put(elementName, element);
        if (debug)
        { log.fine("Mapped element "+elementName+" -> "+element);
        }
      }      
    }
    

    /**
     * Generate a handler set for a root level element into a file
     *   elementName+"Root.frame.xml"
     * 
     * @param element
     * @throws DataException
     * @throws IOException
     */
    // This is only called from the top level, not re-entrant
    private void generateHandlerSet(Tuple element)
      throws DataException,IOException
    {
      
      List<Tuple> elements=new ArrayList<Tuple>();
      String handlerName=localName((String) element.get("elementName"));
      elements.add(element);
      if (errorElementURIs!=null)
      {
        for (URI elementURI:errorElementURIs)
        {
          Tuple errorElement=this.findElementRef(elementURI.toString());
          if (errorElement!=null)
          { elements.add(errorElement);
          }
          else
          { 
            throw new DataException
              ("Error element '"
                +elementURI+"' not found in top level elements"
              );
          }
        }
        
      }
      
      generateHandlerSet(handlerName,elements);
      
    }
    
    /**
     * Generate a root handler set for multiple elements into a file
     *   name+"Root.frame.xml"
     * 
     * @param element
     * @throws DataException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")    
    private void generateHandlerSet(String handlerName,List<Tuple> elements)
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
      
      TypeMapping typeMapping=new TypeMapping();
      typeMapping.handlerTemplate=rootFrame;
 
      // XXX We can give the RootElement a type here
      //   automatically create a root.type with selected elements
      //   or use a pre-built root type
      for (Tuple element : elements)
      {
        Tuple childHandler=makeTopLevelHandler(element,typeMapping);
        if (childHandler!=null)
        { children.add(childHandler);
        }
      }

      handlerStack.pop();
      new DataWriter().writeToURI
        (outputLocation.resolve
          (handlerName+"Root.frame.xml")
          ,rootFrame
          );
      
      
      if (debug)
      {
        log.fine
          ( rootFrame.toText("| ")
          );
      }
      
    }
    
    
    /**
     * Makes a HandlerFrame as a child of the RootFrame 
     * 
     * @param element
     * @param rootType
     * @return
     * @throws DataException
     * @throws IOException
     */
    private EditableArrayTuple makeTopLevelHandler
      (Tuple element
      ,TypeMapping rootType
      )
      throws DataException,IOException
    {
      String elementType=(String) element.get("typeName");
      String elementName=(String) element.get("elementName");
      Tuple inlineType=(Tuple) element.get("inlineType");
      
      if (debug)
      { log.fine("Generating handler for "+elementName);
      }
      
      TypeMapping elementTypeRef=null;
      
      if (elementType!=null)
      { 
        if (debug)
        { log.fine("type is "+elementType);
        }
        elementTypeRef=resolveType(elementType);
      }
      
      
      if (elementTypeRef==null)
      { 
        if (inlineType!=null)
        { 
          elementTypeRef
            =createType(elementName,inlineType);
        }
      }
        
      if (elementTypeRef==null)
      {
         throw new DataException
            ("No type resolved for element "+elementName+" type "+elementType);
          
      }
      else
      {
        EditableArrayTuple childElement
          =makeHandlerFromTemplate(elementTypeRef);
        String elementUri=elementName;
        if (elementUri.startsWith(targetNamespace+"#"))
        { elementUri=localName(elementUri);
        }
        childElement.set("elementURI",elementUri);


        if (rootType.dataType!=null)
        {
          String typeName=rootType.dataType.getURI().toString();
          String fieldName=null;
            
          // XXX Find the field of type elementTypeRef.dataType
          for (Field<?> field: rootType.dataType.getFieldSet().fieldIterable())
          {
            if (field.getType().getURI()
               .equals(elementTypeRef.dataType.getURI())
               )
            { 
              fieldName=field.getName();
              break;
            }
          }
          
          if (fieldName!=null)
          {
            
            childElement.set
              ("assignment"
              ,Expression.create("[:"+typeName+"]."+fieldName)
              );
          }
          else
          {
            throw new DataException
              ("No field in "+rootType.dataType.getURI()+" is of type "
              +elementTypeRef.dataType.getURI()
              );
          }
        }
        
        if (childElement.getType().equals(valueFrameType))
        { childElement.set("type", typeRefTuple(elementTypeRef.dataType));
        }
        return childElement;
      }
       
    }
    
    /**
     * Resolve a type by name, creating it if it doesn't exist
     * 
     * @param elementType
     * @return
     * @throws DataException
     * @throws IOException
     */
    private TypeMapping resolveType(String elementType)
      throws DataException,IOException
    {
      
      TypeMapping ref=typeMap.get(elementType);
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
    
    /**
     *  Create a new type mapping and add it to the map
     * 
     * @param elementType
     * @param xsdType
     * @return
     * @throws DataException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private TypeMapping createType(String elementType,Tuple xsdType)
      throws DataException,IOException
    {
      if (debug)
      { log.fine("Creating type for "+elementType);
      }
      
      TypeMapping ref=new TypeMapping();
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
          // Empty type
          createComplexType(ref);
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
    private void createDeclaredListType(TypeMapping ref,Tuple inlineType)
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
         

      
    }
    
    @SuppressWarnings("unchecked")
    private void createSimpleType(TypeMapping ref)
      throws DataException,IOException
    {
      String itemTypeName=(String) ref.xsdType.get("listItemTypeName");
      if (itemTypeName!=null)
      {
        TypeMapping itemTypeRef=resolveType(itemTypeName);
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
    
    private String cleanTypeName(String typeLocalName)
    {
      if (typeLocalName.endsWith("Type"))
      { typeLocalName=typeLocalName.substring(0,typeLocalName.length()-4);
      }
      if (Character.isLowerCase(typeLocalName.charAt(0)))
      { 
        typeLocalName
          =Character.toUpperCase
            (typeLocalName.charAt(0))+typeLocalName.substring(1);
      }
      typeLocalName=underscoresToCaps(typeLocalName);
      return typeLocalName;
    }
    
    private String underscoresToCaps(String name)
    {
      int upos;
      while ( (upos=name.indexOf('_')) >0)
      { 
        if (upos==name.length()-1)
        { name=name.substring(0,name.length()-1);
        }
        else if (Character.isLetter(name.charAt(upos+1)))
        { 
          name=name.substring(0,upos)
            +Character.toUpperCase(name.charAt(upos+1))
            +(upos+2<name.length()?
               name.substring(upos+2)
               :""
             );
        }
        else
        { break;
        }
          
      }
      return name;
    }
    @SuppressWarnings("unchecked")
    private void createComplexType(TypeMapping ref)
      throws DataException,IOException
    {
      
      
      String typeLocalName
        =cleanTypeName(AbstractFrameHandler.localName(ref.typeName));
      
      
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
        TypeMapping baseType=resolveType(baseTypeName);
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
                  TypeMapping elementType
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
      
      if (baseTypeNamespaceURI!=null)
      {
        // Try to resolve a predefined base type
        URI baseTypeURI=baseTypeNamespaceURI.resolve(typeLocalName);
        try
        { 
          Type baseType=Type.resolve(baseTypeURI);
          dataTypeDecl.set("baseType", typeRefTuple(baseType));
        }
        catch (TypeNotFoundException x)
        {
          if (x.getCause()==null)
          { 
            if (debug)
            { log.log(Level.DEBUG,"Base type not found "+baseTypeURI);
            }
          }
          else
          { throw x;
          }
          
        }
      }


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
      TypeMapping type=null;
      
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
          (localName((String) element.get("elementName"))
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

    /** 
     * Turns a name back into a local name if it is fully qualified (ie. is
     *   an absolute URI)
     * 
     * @param uriName
     * @return The uri fragment, if the uriName is fully qualified
     */
    private String localName(String uriName)
    { 
      URI uri=URI.create(uriName);
      if (uri.isAbsolute())
      { return uri.getFragment();
      }
      else
      { return uriName;
      }
    }
    
    
    @SuppressWarnings("unchecked")
    private Tuple generateFieldFromAttribute
      (Tuple attribute,TypeMapping parent)
      throws DataException,IOException
    {
      String typeName=(String) attribute.get("typeName");
      if (typeName!=null)
      {
        TypeMapping type=resolveType(typeName);
        if (type!=null && type.dataType!=null)
        {
          
          EditableArrayListAggregate<Tuple> bindings
            =(EditableArrayListAggregate<Tuple>) 
              parent.handlerTemplate.get("attributeBindings");
          
          Tuple field=generateField
            (localName((String) attribute.get("name"))
            ,type.dataType
            ,false
            );

          EditableArrayTuple binding
            =new EditableArrayTuple(attributeBindingType);
          binding.set("name",localName((String) attribute.get("name")));
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
      name=underscoresToCaps(name);
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
    
    private Tuple findXsdType(String elementType)
      throws DataException
    { 
      if (targetNamespace!=null
          && !URI.create(elementType).isAbsolute()
         )
      {
        elementType
          =AbstractFrameHandler.combineName
            (targetNamespace,elementType);
      
      }
      return schemaTypeMap.get(elementType);
    }
          
    private Tuple findElementRef(String elementName)
      throws DataException
    {
      if (targetNamespace!=null
          && !URI.create(elementName).isAbsolute()
         )
      {
        elementName
          =AbstractFrameHandler.combineName
            (targetNamespace,elementName);
      
      }
      return schemaElementMap.get(elementName);
    }

    
    /** 
     * Create a new handler for a subtype by copying all the attributes and
     *   children of a base type
     */
    @SuppressWarnings("unchecked")
    private EditableArrayTuple extendHandler(TypeMapping baseType)
      throws DataException
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
    private EditableArrayTuple makeHandlerFromTemplate(TypeMapping ref)
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
    
    
    private EditableArrayTuple makeSimpleHandler(TypeMapping ref)
      throws DataException
    {
      EditableArrayTuple handler
        =new EditableArrayTuple(ref.handlerTemplate);

      return handler;
      
    }
    
    private EditableArrayTuple makeComplexHandler(TypeMapping ref)
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
    private void combineHandlerChildren(TypeMapping ref,HandlerRef handlerRef)
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
    // Called by makeHandlerFromTemplate->makeComplexHandler->combineHandlerChildren
    public Tuple completeHandlerForElement
      (Tuple element
      ,TypeMapping containerRef
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

      TypeMapping elementTypeRef=null;
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
        if (debug)
        { log.fine("Creating in-line type for '"+elementName+"'");
        }
        
        // Name the inline type after the element name
        elementTypeRef=createType(elementName,inlineType);
        
      }
      
      
      if (elementTypeRef!=null)
      { 
        
            
        // Handle recursive cases
        HandlerRef backRef=findInStack(elementTypeRef.typeName);
        if (backRef!=null)
        {
          if (debug)
          { log.fine("Found in stack "+backRef.handler);
          }
          String id=(String) backRef.handler.get("id");
          if (id==null)
          { 
            // We are at the top of the recursive chain
            
            handler.set("id",elementName);
            if (debug)
            { log.fine("Setting id="+elementName);
            }
          }
          else 
          {
            EditableArrayTuple recursiveHandler
              =new EditableArrayTuple(recursiveFrameType);
            recursiveHandler.set("frameId",id);
            return recursiveHandler;
          }
        }
            

        EditableArrayTuple childHandler
          =makeHandlerFromTemplate(elementTypeRef);

        String containingType
          =containerRef.dataType.getURI().toString();

        String elementURI=elementName;
        if (elementURI.startsWith(targetNamespace+"#"))
        { elementURI=localName(elementName);
        }
        childHandler.set("elementURI",elementURI);
            
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
  class TypeMapping
  {
    
    public String typeName;
    public String elementName;
    public Tuple xsdType;
    public Type dataType;
    public Tuple dataTypeDecl;
    public TypeMapping unitTypeRef;
    public TypeMapping baseTypeRef;
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
