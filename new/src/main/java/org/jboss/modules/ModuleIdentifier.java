package org.jboss.modules;

public final class ModuleIdentifier
{
   private final String group;
   private final String artifact;
   private final String version;

   public ModuleIdentifier(final String group, final String artifact, final String version)
   {
      this.group = group;
      this.artifact = artifact;
      this.version = version;
   }

   public String getGroup()
   {
      return group;
   }

   public String getArtifact()
   {
      return artifact;
   }

   public String getVersion()
   {
      return version;
   }

   @Override
   public boolean equals(final Object o)
   {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;

      final ModuleIdentifier moduleId = (ModuleIdentifier) o;

      if(group != null ? !group.equals(moduleId.group) : moduleId.group != null) return false;
      if(artifact != null ? !artifact.equals(moduleId.artifact) : moduleId.artifact != null) return false;
      if(version != null ? !version.equals(moduleId.version) : moduleId.version != null) return false;

      return true;
   }

   @Override
   public int hashCode()
   {
      int result = group != null ? group.hashCode() : 0;
      result = 31 * result + (artifact != null ? artifact.hashCode() : 0);
      result = 31 * result + (version != null ? version.hashCode() : 0);
      return result;
   }

   @Override
   public String toString()
   {
      return String.format("Module:%s:%s:%s",group, artifact, version);
   }
}