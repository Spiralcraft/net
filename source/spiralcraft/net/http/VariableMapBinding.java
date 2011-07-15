//
//Copyright (c) 1998,2007 Michael Toth
//Spiralcraft Inc., All Rights Reserved
//
//This package is part of the Spiralcraft project and is licensed under
//a multiple-license framework.
//
//You may not use this file except in compliance with the terms found in the
//SPIRALCRAFT-LICENSE.txt file at the top of this distribution, or available
//at http://www.spiralcraft.org/licensing/SPIRALCRAFT-LICENSE.txt.
//
//Unless otherwise agreed to in writing, this software is distributed on an
//"AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
//
package spiralcraft.net.http;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spiralcraft.log.Level;
import spiralcraft.log.ClassLog;

import spiralcraft.lang.BindException;
import spiralcraft.lang.Channel;
import spiralcraft.lang.CollectionDecorator;


import spiralcraft.text.ParseException;
import spiralcraft.text.translator.Translator;

import spiralcraft.util.ArrayUtil;
import spiralcraft.util.string.StringConverter;

import spiralcraft.lang.IterationDecorator;

/**
 * <p>Associates a request or form VariableMap with a target Channel.
 * </p>
 * 
 * @author mike
 *
 */
@SuppressWarnings({"unchecked","rawtypes"}) // Casts related to StringConverter and arrays
public class VariableMapBinding<Tvar>
{
  private static final ClassLog log
    =ClassLog.getInstance(VariableMapBinding.class);

  private final Channel<Tvar> targetChannel;
  private final String name;
  private StringConverter converter;
  private boolean array;
  private IterationDecorator iterable;
  private CollectionDecorator collectionDecorator;
  private Class<Tvar> clazz;
  private boolean passNull;
  private boolean debug;
  private Translator translator;  
  private boolean trim;
  
  public VariableMapBinding
    (Channel<Tvar> targetChannel
    ,String name
    ,StringConverter sconverter
    )
    throws BindException
  {
    this.targetChannel=targetChannel;
    this.name=name;
    this.converter=sconverter;
    clazz=targetChannel.getContentType();
     
    // Prefer to handle aggregate types using the component type due to
    //   the fact we can't always rely on a comma delimited list, which
    //   will be the default for an array
    if (this.converter==null)
    { 
      array=targetChannel.getContentType().isArray();
      collectionDecorator
        =targetChannel.<CollectionDecorator>decorate(CollectionDecorator.class);
      iterable
        =targetChannel.<IterationDecorator>decorate(IterationDecorator.class);
      if (iterable!=null)
      { 
        
        this.converter=
          iterable.getComponentReflector().getStringConverter();
        if (this.converter==null)
        { 
          this.converter=StringConverter.getInstance
            (iterable.getComponentReflector().getContentType());
        }
      }
        
    }
    if (this.converter==null)
    { this.converter=targetChannel.getReflector().getStringConverter();
    }
    if (this.converter==null)
    { 
      this.converter
        =StringConverter.getInstance(targetChannel.getContentType());
    }
    
    if (this.converter==null && clazz!=String.class)
    { 
      throw new BindException
        ("Auto-conversion failed- can't resolve StringConverter for "
        +clazz.getName()+" and no explicit converter supplied"
        );
    }
  }

  /**
   * Trim whitespace from input before converting
   * 
   * @param trim
   */
  public void setTrim(boolean trim)
  { this.trim=trim;
  }
  
  /**
   * <p>Specify a Translator which sits between the VariableMap and the
   *   StringConverter and normalizes data read from the VariableMap and
   *   published from the target. 
   * </p>
   * 
   * @param translator
   */
  public void setTranslator(Translator translator)
  { this.translator=translator;
  }
  
  public void setDebug(boolean debug)
  { this.debug=debug;
  }
  
  /**
   * <p>Whether the binding will set the target value to null of the bound
   *   request variable is not present
   * </p>
   * @param passNull
   */
  
  public void setPassNull(boolean passNull)
  { this.passNull = passNull;
  }
  
  /**
   * <p>Specifies the StringConverter which will provide the bidirectional
   *   conversion from a String to the native type of the binding target
   * </p>
   * 
   * <p>Note that for array types, the Converter should be specified for
   *   the array component type, as the array elements are unpacked for
   *   use in the VariableMap.
   * </p> 
   * @param converter
   */
  public void setConverter(StringConverter converter)
  { this.converter=converter;
  }
  
  private Object translateValueIn(String val)
  {
    if (val==null)
    { return null;
    }
    
    String tval=val;
    if (translator!=null)
    { 
      try
      { tval=translator.translateIn(val);
      }
      catch (ParseException x)
      {
        // XXX This method should throw something explicit
        log.log(Level.WARNING,"Error translating "+val,x);
        throw new RuntimeException(x);
      }
    }
    
    if (converter!=null)
    { 
      if (debug)
      { log.fine("Converting "+tval+" with "+converter.toString());
      }
      return converter.fromString(tval);
    }
    else
    { 
      if (debug)
      { log.fine("Not converting "+tval);
      }      
      return tval;
    }
  }
  
  
  private String translateValueOut(Object val)
  {
    if (val==null)
    { return null;
    }
    
    String sval;
    if (converter!=null)
    { sval=converter.toString(val);
    }
    else
    { sval=(String) val;
    }
    
    String tval=sval;
    if (translator!=null)
    { 
      try
      { tval=translator.translateOut(sval);
      }
      catch (ParseException x)
      {
        // XXX This method should throw something explicit
        log.log(Level.WARNING,"Error translating "+sval,x);
        throw new RuntimeException(x);
      }
        
    }
    return tval;
  }
  
  /**
   * <p>Translate the value from the target into a List<String> for publishing
   *   to the URI query string.
   * </p>
   * 
   * <p>Since URL-encoded variables can have multiple values, a List<String>
   *   is always used, even if the variable has a single value.
   * </p>
   * 
   * <p>The String values returned from this method must be further encoded
   *   for inclusion in the URL.
   * </p>
   * 
   * @return A List of String-encoded values associated with the variable.
   */
  public List<String> translate()
  {
    if (array)
    {
      Tvar array=targetChannel.get();
      if (debug)
      { 
        log.fine
          ("Translating : "
          +ArrayUtil.format(array, "," ,"\"")
          );
      }
        
      if (array==null)
      { return null;
      }
      else
      {
        int len = Array.getLength(array);
        List<String> ret=new ArrayList<String>(len);
        for (int i=0;i<len;i++)
        { 
          Object val=Array.get(array,i);
          String sval=translateValueOut(val);
          if (sval!=null)
          { ret.add(sval);
          }
        }
        return ret;
      }
    }
    else if (iterable!=null)
    { 
      Iterator it=iterable.iterator();
      if (it==null)
      { return null;
      }
      else
      {
        List<String> ret=new ArrayList<String>();   
        while (it.hasNext())
        {
          Object val=it.next();
          String sval=translateValueOut(val);
          if (sval!=null)
          { ret.add(sval);
          }
        }
        return ret;
      }
    }
    else
    {
      Tvar val=targetChannel.get();
      if (debug)
      { 
        log.fine
          ("Translating : "
          +val
          );
      }
      String sval=translateValueOut(val);
      if (sval==null)
      { return null;
      }
      else
      {
        List<String> ret=new ArrayList<String>(1);
        ret.add(sval);
        return ret;
      }
    }
  }
  
  private String preprocess(String val)
  {
    if (val!=null && trim)
    { val=val.trim();
    }
    return val;
  }
  
  /**
   * <p>Read data from the map and write it to the target channel.
   * </p>
   * 
   * <p>The VariableMap represents data read from an HTTP request in the form
   *   of a set of variable names mapped to one or more values.
   * </p>
   * 
   * <p>
   * @param map
   */
  public void read(VariableMap map)
  {
    readValues(map!=null?map.get(name):null);
  }
  
  /**
   * <p>Translate the strings to a native value and write it to the target
   *   channel.
   * </p>
   * 
   * <p>The VariableMap represents data read from an HTTP request in the form
   *   of a set of variable names mapped to one or more values.
   * </p>
   * 
   * <p>
   * @param map
   */
  public void readValues(List<String> vals)
  { 
    Tvar value=convertInput(vals);
    if (value!=null || passNull)
    { 
      if (debug)
      { log.fine("Setting target to "+value);
      }
      if (!targetChannel.set(value))
      { log.warning("Assignment failed for "+name);
      }
    }
  }
  
  public Tvar convertInput(List<String> vals)
  {
    if (debug)
    { log.fine("Reading "+vals+" for "+name);
    }
    
    
    if (vals!=null && vals.size()>0)
    { 

      if (array)
      {
        try
        {
          Object array=Array.newInstance(clazz.getComponentType(),0);
          array=ArrayUtil.expandBy(array, vals.size());
          int i=0;
          for (String val : vals)
          { 
            val=preprocess(val);
            Array.set(array, i++, translateValueIn(val));
          }

          return (Tvar) array;
        }
        catch (IllegalArgumentException x)
        {
          throw new IllegalArgumentException
            ("Error making array object for "
            +clazz.getComponentType()+" to hold "+vals
            ,x);
        }
          
      }
      else if (collectionDecorator!=null)
      {
        Object collection=collectionDecorator.newCollection();
        for (String val:vals)
        { 
          val=preprocess(val);
          collectionDecorator.add(collection,translateValueIn(val));
        }
        return (Tvar) collection;
        
      }
      else
      { 
        Object value=translateValueIn(preprocess(vals.get(0)));
        return (Tvar) value;
      }
        
    }
    else 
    { 
      if (debug)
      { log.fine("convert to null for "+name);
      }
      return null;
    }
  }
  

  
  
}
