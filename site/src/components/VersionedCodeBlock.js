import CodeBlock from '@theme/CodeBlock';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

export default function VersionedCodeBlock({language, children}) {
  const {siteConfig} = useDocusaurusContext();

  return (
    <CodeBlock language={language}>
      {children(siteConfig.customFields)}
    </CodeBlock>
  );
}
