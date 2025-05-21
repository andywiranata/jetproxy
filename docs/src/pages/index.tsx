import React, { useEffect } from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';
import Heading from '@theme/Heading';
import Head from '@docusaurus/Head';

import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  useEffect(() => {
    if (typeof window.gtag === 'function') {
      window.gtag('event', 'conversion', {
        send_to: 'AW-962981890/Cf0vCMbzjcsaEILgl8sD',
      });
    }
  }, []);

  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">
          {siteConfig.tagline || 'Fast, lightweight reverse proxy powered by Jetty – configure in YAML, deploy in seconds.'}
        </p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/intro">
            🚀 Get Started in 5 Minutes
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home(): JSX.Element {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} – Proxy With Simplicity`}
      description="JetProxy is a fast and flexible reverse proxy built on Jetty. Secure, route, and transform HTTP traffic with YAML configuration.">
      <Head>
        <meta name="keywords" content="JetProxy, Java Proxy, Jetty Reverse Proxy, API Gateway, YAML Proxy, Middleware, Caching Proxy, Traefik Alternative" />
        <meta name="author" content="JetProxy Contributors" />
        <meta property="og:type" content="website" />
        <meta property="og:title" content="JetProxy – Lightweight Java Reverse Proxy" />
        <meta property="og:description" content="JetProxy is a developer-first proxy powered by Jetty. Simple YAML config, secure auth, flexible routing, and fast deployments." />
        <meta property="og:image" content="https://jetproxy.andywiranata.me/assets/images/jetproxy-intro-3a53dc6772cf521d3d37312d672cf6f7.png" />
        <meta property="og:url" content="https://jetproxy.andywiranata.me" />
        {/* <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:title" content="JetProxy – Lightweight Java Reverse Proxy" />
        <meta name="twitter:description" content="Secure and route APIs with JetProxy, built on Jetty. Middleware-first design with YAML simplicity." />
        <meta name="twitter:image" content="https://jetproxy.andywiranata.me/assets/images/jetproxy-intro-3a53dc6772cf521d3d37312d672cf6f7.png" /> */}
      </Head>
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
    </Layout>
  );
}
