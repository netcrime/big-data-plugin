/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.pentaho.big.data.kettle.plugins.formats.impl.avro.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.big.data.api.cluster.NamedCluster;
import org.pentaho.big.data.api.cluster.NamedClusterService;
import org.pentaho.big.data.api.cluster.service.locator.NamedClusterServiceLocator;
import org.pentaho.big.data.kettle.plugins.formats.FormatInputOutputField;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.plugins.Plugin;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginMainClassType;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaPluginType;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.metastore.api.IMetaStore;

@RunWith( MockitoJUnitRunner.class )
public class AvroInputMetaTest {

  @Mock
  private NamedClusterServiceLocator namedClusterServiceLocator;

  @Mock
  private NamedClusterService namedClusterService;

  @Mock
  private StepMeta nextStep;

  @Mock
  private VariableSpace space;

  @Mock
  private IMetaStore metaStore;

  @Mock
  private RowMetaInterface rowMeta;

  @Mock
  private FormatInputOutputField field;

  @Mock
  private Repository repository;

  @Mock
  private PluginInterface pluginInterface;

  private RowMetaInterface[] info;

  /**
   * name of current step to set to value metadata
   */
  private String origin = "Avro Input";

  private AvroInputMeta meta;

  @Before
  public void setUp() throws KettlePluginException {
    meta = new AvroInputMeta( namedClusterServiceLocator, namedClusterService );
    when( field.getType() ).thenReturn( ValueMetaInterface.TYPE_STRING );

    Map<Class<?>, String> classMap = new HashMap<Class<?>, String>();
    Class<? extends PluginTypeInterface> pluginType = ValueMetaPluginType.class;
    PluginMainClassType mainClassTypesAnnotation = pluginType.getAnnotation( PluginMainClassType.class );
    classMap.put( mainClassTypesAnnotation.value(), ValueMetaString.class.getName() );

    pluginInterface = new Plugin( new String[] { String.valueOf( ValueMetaInterface.TYPE_STRING ) },
        ValueMetaPluginType.class,
        ValueMetaPluginType.class.getAnnotation( PluginMainClassType.class ).value(),
        "", "", "", null, false, false, classMap, new ArrayList<String>(), null, null );

    PluginRegistry.getInstance().registerPlugin( ValueMetaPluginType.class, pluginInterface );
  }

  @Test
  public void testGetFields_clearPreviousFileds() throws KettleStepException {
    meta.getFields( rowMeta, origin, info, nextStep, space, repository, metaStore );
    verify( rowMeta ).clear();
  }

  @Test
  public void testGetFields_originShouldBeSetedToRowMeta() throws KettleStepException {
    meta.setInputFields( Arrays.asList( field ) );
    meta.getFields( rowMeta, origin, info, nextStep, space, repository, metaStore );
    ArgumentCaptor<ValueMetaInterface> argument = ArgumentCaptor.forClass( ValueMetaInterface.class );
    verify( rowMeta ).addValueMeta( argument.capture() );
    assertEquals( origin, argument.getValue().getOrigin() );
  }

  @Test
  public void testGetFields_theNameWasUpdatedByVariables() throws KettleStepException {
    meta.setInputFields( Arrays.asList( field ) );
    meta.getFields( rowMeta, origin, info, nextStep, space, repository, metaStore );
    verify( space ).environmentSubstitute( anyString() );
  }

  @Test
  public void testGetFields_infoMetaShouldBeMerged_ifWePassingFieldsThroughStep() throws KettleStepException {
    meta.inputFiles.passingThruFields = true;

    RowMetaInterface forMerge = mock( RowMetaInterface.class );
    RowMetaInterface[]  rmi = new RowMetaInterface[] { forMerge };
    meta.setInputFields( Arrays.asList( field ) );
    meta.getFields( rowMeta, origin, rmi, nextStep, space, repository, metaStore );
    verify( rowMeta ).mergeRowMeta( eq( forMerge ), eq( origin ) );
  }

  @Test
  public void testGetFields_infoMetaShouldNotBeMerged_ifWeDoNotHaveadditionalFields() throws KettleStepException {
    meta.inputFiles.passingThruFields = true;

    meta.setInputFields( Arrays.asList( field ) );
    meta.getFields( rowMeta, origin, info, nextStep, space, repository, metaStore );
    verify( rowMeta, never() ).mergeRowMeta( any( RowMetaInterface.class ), anyString() );
  }

  @Test( expected = KettleStepException.class )
  public void testGetFields_unknownPluginForFieldType() throws KettleStepException {
    FormatInputOutputField fld = mock( FormatInputOutputField.class );

    meta.setInputFields( Arrays.asList( fld ) );
    meta.getFields( rowMeta, origin, info, nextStep, space, repository, metaStore );
  }

  @Test
  public void testGetNamedCluster() throws KettleStepException {
    NamedCluster nc = meta.getNamedCluster();
    verify( namedClusterService ).getClusterTemplate();
    //since namedClusterService is mock it should return null
    assertNull( nc );
  }

  @Test
  public void testGetNamedClusterServiceLocator() throws KettleStepException {
    assertEquals( namedClusterServiceLocator, meta.getNamedClusterServiceLocator() );
  }

}