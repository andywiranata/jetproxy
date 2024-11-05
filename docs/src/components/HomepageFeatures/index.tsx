import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';


type FeatureItem = {
  title: string;
  img: string;
  description: JSX.Element;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Simple to Configure',
    img: require('@site/static/img/undraw_copybara_mountain.png').default,
    description: (
      <>
        JetProxy is designed for effortless setup, 
        allowing you to quickly integrate proxy features and customize them to suit your needs with minimal configuration.
      </>
    ),
  },
  {
    title: 'Focus on What Matters',
    img: require('@site/static/img/undraw_copybara_mountain_focus.png').default,
    description: (
      <>
        JetProxy includes built-in HTTP caching, helping to reduce server load 
        and improve response times by storing frequently requested content.
      </>
    ),
  },
  {
    title: 'Built for Flexibility',
    img:  require('@site/static/img/undraw_copybara_mountain.png').default,
    description: (
      <>
        Powered by a modern backend, JetProxy allows you to extend and customize functionality,
        enabling flexible routing rules and alternative authentication mechanisms without the overhead.
      </>
    ),
  },
];

function Feature({title, img, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <img className={styles.featureSvg} 
        role="img" src={img} />
      </div>
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): JSX.Element {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
